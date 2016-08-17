import traceback, sys, pickle, json, time, base64, os, hashlib, binascii
import axolotl_curve25519 #https://github.com/tgalal/python-axolotl-curve25519
from twisted.web import server, resource, http
from util import *

class InvalidSignature(Exception):
    def __init__(self, message):
        super(InvalidSignature, self).__init__(message)

#Verify that the POST body has signed the client certificate used
def validateRequest(log, clientCertHex, jsonBody, requireData=False):
    if 'signature' not in jsonBody:
        raise Exception("POST missing signature")
    if 'identityKey' not in jsonBody:
        raise Exception("POST missing identityKey")
    if 'timestamp' not in jsonBody:
        raise Exception("POST missing timestamp")
    if len(str(jsonBody['timestamp'])) != 10:
        raise Exception("Timestamp is unexpected size")
    if time.time() - (60 * 60 * 24 * 3) > jsonBody['timestamp']:
        raise Exception("Timestamp is too old")
    if 'signatureVersion' not in jsonBody:
        raise Exception("POST missing signatureVersion")
    if jsonBody['signatureVersion'] != 1:
        raise Exception("Unknown signatureVersion")
    if 'data' not in jsonBody and requireData:
        raise Exception("POST missing data")
    if len(clientCertHex) != 64:
        raise Exception("Client Certificate is unexpected size")
    if 'certFingerprint' in jsonBody:
        if jsonBody['certFingerprint'] != clientCertHex:
            raise Exception("The certFingerprint included did not match the one in the TLS connection.", clientCertHex, jsonBody['certFingerprint'])

    forSigning = chr(1) + str(jsonBody['timestamp']) + binascii.unhexlify(clientCertHex)
    if 0 != axolotl_curve25519.verifySignature(binascii.unhexlify(jsonBody['identityKey']), forSigning, base64.b64decode(jsonBody['signature'])):
        raise InvalidSignature("Signature Verify Failed")
    log.info("  Matching Client Cert {ccid} to identity {idkey}", ccid=clientCertHex[0:10], idkey=jsonBody['identityKey'][0:10])
    
class V1API(resource.Resource):
    pass

class V1BugHandler(resource.Resource):
    def __init__(self, db, log):
        self.db = db
        self.log = log
        resource.Resource.__init__(self)
    def getChild(self, name, request):
        return V1BugHandler_impl(self.db, self.log, name)
class V1BugHandler_impl(resource.Resource):
    def __init__(self, db, log, uuid):
        resource.Resource.__init__(self)
        self.db = db
        self.log = log
        self.uuid = uuid
    def render_PUT(self, request):
        self.log.info("v1 reporting bug {uuid}", uuid=self.uuid)
        self.log.warn("v1 reporting bug")
        r = BugReporter.Forward(self.uuid, request.content.read())
        if r.status_code != requests.codes.created:
            self.log.info("v1 reporting bug: error reporting bug {uuid}: {txt}", uuid=self.uuid, txt=str(r.text))
            self.log.error("v1 reporting bug: error reporting bug: {txt}", txt=str(r.text))
            return "200: Success, but an error talking to the bug database"
        else:
            self.log.info("v1 reporting bug: success reporting bug {uuid}: {txt}", uuid=self.uuid, txt=str(r.text))
            self.log.warn("v1 reporting bug: success reporting bug")
            return "200: Success"

class V1RegisterHandler(resource.Resource):
    def __init__(self, db, log):
        self.db = db
        self.log = log
        resource.Resource.__init__(self)
    def render_POST(self, request):
        ccIDHex = request.transport.getPeerCertificate().digest("SHA256").replace(":", "")
        self.log.info("v1 Register request by Client Cert {ccid}", ccid=ccIDHex[0:10])
        self.log.warn("v1 Register request")
        submittedData = request.content.read()
        try:
            jsonBody = json.loads(submittedData)
            validateRequest(self.log, ccIDHex, jsonBody)
        except:
            tb = sys.exc_info()
            self.log.error("v1 Register request: Problem handling request. body={data} errortype={t} error={e}", data=submittedData, t=tb[0], e=base64.b64encode(traceback.format_exc()))
            request.setResponseCode(500)
            return "500: Error with request"

        #Check if this key is already registered, if not, create the index
        if self.db.get(jsonBody['identityKey']) != None:
            self.log.info("v1 Register request: {idkey} Already registered", idkey=jsonBody['identityKey'][0:10])
            self.log.error("v1 Register request: Already registered")
            request.setResponseCode(200)
            return "200: Already Registered"
        else:
            self.log.info("v1 Register request: {idkey} New registration", idkey=jsonBody['identityKey'][0:10])
            self.log.warn("v1 Register request: New registration")
            self.db.put(jsonBody['identityKey'], pickle.dumps(set()))
            
            request.setResponseCode(201)
            return "201: Success"

class V1TokenHandler(resource.Resource):
    def __init__(self, db, log):
        resource.Resource.__init__(self)
        self.db = db
        self.log = log
    def getChild(self, name, request):
        return V1TokenHandler_impl(self.db, self.log, name)
class V1TokenHandler_impl(resource.Resource):
    def __init__(self, db, log, tokenID):
        resource.Resource.__init__(self)
        self.db = db
        self.log = log
        self.tokenID = tokenID
    def render_POST(self, request):
        ccIDHex = request.transport.getPeerCertificate().digest("SHA256").replace(":", "")
        self.log.info("v1 Token request by Client Cert {ccid}", ccid=ccIDHex[0:10])
        self.log.warn("v1 Token request")
        submittedData = request.content.read()
        try:
            jsonBody = json.loads(submittedData)
            validateRequest(self.log, ccIDHex, jsonBody)
        except:
            tb = sys.exc_info()
            self.log.error("v1 Token request: Problem handling request. body={data} errortype={t} error={e}", data=submittedData, t=tb[0], e=base64.b64encode(traceback.format_exc()))
            request.setResponseCode(500)
            return "500: Error with request"
        
        index = self.db.get(jsonBody['identityKey'])
        if index == None:
            self.log.info("v1 Token request: {idkey} not registered", idkey=jsonBody['identityKey'][0:10])
            self.log.error("v1 Token request: not registered")
            request.setResponseCode(406)
            return "406: Register First"

        if len(self.tokenID) == 0: 
            self.log.info("v1 Token request: {idkey} no token ID specified", idkey=jsonBody['identityKey'][0:10])
            self.log.error("v1 Token request: no token ID specified")
            request.setResponseCode(500)
            return "500: No tokenID specified"
        
        data = self.db.get(jsonBody['identityKey'] + "_token")
        #Don't care if it exists or not, store it, maybe overwrite it
        self.db.put(jsonBody['identityKey'] + "_token", self.tokenID)
        
        self.log.info("v1 Token request: {idkey} stored token ID: {tokenID}", idkey=jsonBody['identityKey'][0:10], tokenID=self.tokenID)
        request.setResponseCode(200)
        return "200: Success"

class V1DeregisterHandler(resource.Resource):
    def __init__(self, db, log):
        self.db = db
        self.log = log
        resource.Resource.__init__(self)
    def render_POST(self, request):
        ccIDHex = request.transport.getPeerCertificate().digest("SHA256").replace(":", "")
        self.log.info("v1 Deregister request by Client Cert {ccid}", ccid=ccIDHex[0:10])
        self.log.warn("v1 Deregister request")
        submittedData = request.content.read()
        try:
            jsonBody = json.loads(submittedData)
            validateRequest(self.log, ccIDHex, jsonBody)
        except:
            tb = sys.exc_info()
            self.log.error("v1 Deregister request: Problem handling request. body={data} errortype={t} error={e}", data=submittedData, t=tb[0], e=base64.b64encode(traceback.format_exc()))
            request.setResponseCode(500)
            return "500: Error with request"
        
        #Check if this key is already registered - if it is, delete its index and messages
        res = self.db.get(jsonBody['identityKey'])
        if res == None:
            self.log.info("v1 Deregister request: {idkey} not registered", idkey=jsonBody['identityKey'][0:10])
            self.log.error("v1 Deregister request: not registered")
            request.setResponseCode(200)
            return "200: Not Registered"
        else:
            #Delete each message
            cntMsgs = 0
            pendingMessages = pickle.loads(res)
            for m in pendingMessages:
                self.db.delete(m + "_msg")
                cntMsgs += 1
            self.log.info("v1 Deregister request: {idkey} deleted {cnt} messages", idkey=jsonBody['identityKey'][0:10], cnt=cntMsgs)
            self.log.warn("v1 Deregister request: deleted {cnt} messages", cnt=cntMsgs)
            
            #Delete the index
            self.db.delete(jsonBody['identityKey'] + "_token")
            self.db.delete(jsonBody['identityKey'])
            request.setResponseCode(204)
            return "204: Success"
            
class V1PollHandler(resource.Resource):
    def __init__(self, db, log):
        self.db = db
        self.log = log
        resource.Resource.__init__(self)
    def render_POST(self, request):
        ccIDHex = request.transport.getPeerCertificate().digest("SHA256").replace(":", "")
        self.log.info("v1 Poll request by Client Cert {ccid}", ccid=ccIDHex[0:10])
        self.log.warn("v1 Poll request")
        submittedData = request.content.read()
        try:
            jsonBody = json.loads(submittedData)
            validateRequest(self.log, ccIDHex, jsonBody)
        except:
            tb = sys.exc_info()
            self.log.error("v1 Poll request: Problem handling request. body={data} errortype={t} error={e}", data=submittedData, t=tb[0], e=base64.b64encode(traceback.format_exc()))
            request.setResponseCode(500)
            return "500: Error with request"
        
        #Look up the KeyID
        index = self.db.get(jsonBody['identityKey'])
        if index == None:
            self.log.info("v1 Poll request: {idkey} not registered", idkey=jsonBody['identityKey'][0:10])
            self.log.error("v1 Poll request: not registered")
            request.setResponseCode(406)
            return "406: Register First"
        pendingRetrievals = pickle.loads(index)
        
        #Send all the message IDs pending retrieval
        response = []
        for m in pendingRetrievals:
            response.append(m)
        
        self.log.info("v1 Poll request: {idkey} successful poll for {cnt} messages", idkey=jsonBody['identityKey'][0:10], cnt=len(response))
        self.log.warn("v1 Poll request: successful poll for {cnt} messages", cnt=len(response))
        request.setResponseCode(200)
        return json.dumps(response)

class V1GetHandler(resource.Resource):
    def __init__(self, db, log):
        resource.Resource.__init__(self)
        self.db = db
        self.log = log
    def getChild(self, name, request):
        return V1GetHandler_impl(self.db, self.log, name)
class V1GetHandler_impl(resource.Resource):
    def __init__(self, db, log, messageID):
        resource.Resource.__init__(self)
        self.db = db
        self.log = log
        self.messageID = messageID
    def render_POST(self, request):
        ccIDHex = request.transport.getPeerCertificate().digest("SHA256").replace(":", "")
        self.log.info("v1 Get request by Client Cert {ccid}", ccid=ccIDHex[0:10])
        self.log.warn("v1 Get request")
        submittedData = request.content.read()
        try:
            jsonBody = json.loads(submittedData)
            validateRequest(self.log, ccIDHex, jsonBody)
        except:
            tb = sys.exc_info()
            self.log.error("v1 Get request: Problem handling request. body={data} errortype={t} error={e}", data=submittedData, t=tb[0], e=base64.b64encode(traceback.format_exc()))
            request.setResponseCode(500)
            return "500: Error with request"
        
        index = self.db.get(jsonBody['identityKey'])
        if index == None:
            self.log.info("v1 Get request: {idkey} not registered", idkey=jsonBody['identityKey'][0:10])
            self.log.error("v1 Get request: not registered")
            request.setResponseCode(406)
            return "406: Register First"

        if len(self.messageID) == 0: 
            self.log.info("v1 Get request: {idkey} no message ID specified", idkey=jsonBody['identityKey'][0:10])
            self.log.error("v1 Get request: no message ID specified")
            request.setResponseCode(500)
            return "500: No messageID specified"
        elif len(self.messageID) != 64: 
            self.log.info("v1 Get request: {idkey} invalid message ID {msgid}", idkey=jsonBody['identityKey'][0:10], msgid=self.messageID)
            self.log.error("v1 Get request: invalid message ID {msgid}", msgid=self.messageID)
            request.setResponseCode(500)
            return "500: Invalid messageID specified"
        
        #SEC: One cannot impersonate someone's KeyID, so one cannot retrieve
        #     someone else's message
        data = self.db.get(self.messageID + "_msg")
        if data == None:
            self.log.info("v1 Get request: {idkey} message ID {msgid} not found", idkey=jsonBody['identityKey'][0:10], msgid=self.messageID[0:10])
            self.log.error("v1 Get request: message not found")
            request.setResponseCode(404)
            return "404: Not Found"
        
        #Send it
        #TODO: Handle Range Requests
        self.log.info("v1 Get request: {idkey} sent message {msgid}, length {len}", idkey=jsonBody['identityKey'][0:10], msgid=self.messageID[0:10], len=len(data))
        self.log.warn("v1 Get request: sent message, length {len}", len=len(data))
        request.responseHeaders.addRawHeader(b"content-type", b"application/octet-stream")
        request.setResponseCode(200)
        return data

class V1DeleteHandler(resource.Resource):
    def __init__(self, db, log):
        resource.Resource.__init__(self)
        self.db = db
        self.log = log
    def getChild(self, name, request):
        return V1DeleteHandler_impl(self.db, self.log, name)
class V1DeleteHandler_impl(resource.Resource):
    def __init__(self, db, log, messageID):
        resource.Resource.__init__(self)
        self.db = db
        self.log = log
        self.messageID = messageID
    def render_POST(self, request):
        ccIDHex = request.transport.getPeerCertificate().digest("SHA256").replace(":", "")
        self.log.info("v1 Delete request by Client Cert {ccid}", ccid=ccIDHex[0:10])
        self.log.warn("v1 Delete request")
        submittedData = request.content.read()
        try:
            jsonBody = json.loads(submittedData)
            validateRequest(self.log, ccIDHex, jsonBody)
        except:
            tb = sys.exc_info()
            self.log.error("v1 Delete request: Problem handling request. body={data} errortype={t} error={e}", data=submittedData, t=tb[0], e=base64.b64encode(traceback.format_exc()))
            request.setResponseCode(500)
            return "500: Error with request"
        
        index = self.db.get(jsonBody['identityKey'])
        if index == None:
            self.log.info("v1 Delete request: {idkey} not registered", idkey=jsonBody['identityKey'][0:10])
            self.log.error("v1 Delete request: not registered")
            request.setResponseCode(406)
            return "406: Register First"
            
        if len(self.messageID) == 0:
            self.log.info("v1 Delete request: {idkey} no message specified", idkey=jsonBody['identityKey'][0:10])
            self.log.error("v1 Delete request: no message specified")
            request.setResponseCode(500)
            return "500: No messageID specified"
        elif len(self.messageID) != 64: 
            self.log.info("v1 Delete request: {idkey} invalid message ID {msgid} specified", idkey=jsonBody['identityKey'][0:10], msgid=self.messageID)
            self.log.error("v1 Delete request: invalid message ID {msgid} specified", msgid=self.messageID)
            request.setResponseCode(500)
            return "500: Invalid messageID specified"
        
        #SEC: One cannot impersonate someone's KeyID, so one cannot delete
        #     someone else's message
        #Delete the pending message from the user's index
        #TODO: Obvious race condition
        pendingRetrievals = pickle.loads(index)
        if self.messageID in pendingRetrievals:
            pendingRetrievals.remove(self.messageID)
            self.db.put(jsonBody['identityKey'], pickle.dumps(pendingRetrievals))
        
        #Look up the message from the KeyID + MessageID, delete it
        self.db.delete(self.messageID + "_msg")
        
        self.log.info("v1 Delete request: {idkey} successful deletion of {msgid}", idkey=jsonBody['identityKey'][0:10], msgid=self.messageID)
        request.setResponseCode(204)
        return ""
        
class V1PostHandler(resource.Resource):
    def __init__(self, db, log):
        self.db = db
        self.log = log
        resource.Resource.__init__(self)
    def render_POST(self, request):
        ccIDHex = request.transport.getPeerCertificate().digest("SHA256").replace(":", "")
        self.log.info("v1 POST request by Client Cert {ccid}", ccid=ccIDHex[0:10])
        self.log.warn("v1 POST request")
        submittedData = request.content.read()
        try:
            jsonBody = json.loads(submittedData)
            validateRequest(self.log, ccIDHex, jsonBody, True)
        except:
            tb = sys.exc_info()
            self.log.error("v1 POST request: Problem handling request. body={data} errortype={t} error={e}", data=submittedData, t=tb[0], e=base64.b64encode(traceback.format_exc()))
            request.setResponseCode(500)
            return "500: Error with request"
        
        #SEC: We prevent unauth DoS of a user because no one can guess their 
        #     KeyID unless it was given to them in person or by an introduction
        photoData = base64.b64decode(jsonBody['data'])
        if len(photoData) < 65:
            self.log.info("v1 POST request: {idkey} tried to post a tiny message: {len} bytes!", idkey=jsonBody['identityKey'][0:10], len=len(photoData))
            self.log.error("v1 POST request: tried to post a tiny message: {len} bytes!", len=len(photoData))
            request.setResponseCode(500)
            return "500: Message too short"

        #Read the first 256 bits off the wire; convert them to a KeyID
        recipientKeyId = bytes_to_asciihex(photoData[0:32])
        if recipientKeyId == jsonBody['identityKey']:
            self.log.info("v1 POST request: {idkey} tried to post to themself", idkey=jsonBody['identityKey'][0:10])
            self.log.error("v1 POST request: tried to post to themself")
            request.setResponseCode(500)
            return "500: Cannot post to yourself"
            
        recipient = self.db.get(recipientKeyId)
        if recipient == None:
            self.log.info("v1 POST request: {idkey} recipient {recip} not found", idkey=jsonBody['identityKey'][0:10], recip=recipientKeyId[0:10])
            self.log.error("v1 POST request: recipient not found")
            request.setResponseCode(404)
            return "404: Recipient Not Found"
        
        #Generate the MessageID. Make it deterministic so the same message will not be duplicated
        messageID = hashlib.sha256(photoData[32:]).hexdigest().upper()
        
        #Store the message in the database
        if self.db.get(messageID + "_msg") == None:
            self.db.put(messageID + "_msg", photoData[32:])
        else:
            self.log.info("v1 POST request: {idkey} messageID {mid} was already in database", idkey=jsonBody['identityKey'][0:10], mid=messageID[0:10])
            self.log.error("v1 POST request: message was already in database")

        #Update the recipient's index
        #TODO: Obvious race condition
        pendingRetrievals = pickle.loads(self.db.get(recipientKeyId))
        if messageID not in pendingRetrievals:
            pendingRetrievals.add(messageID)
            self.db.put(recipientKeyId, pickle.dumps(pendingRetrievals))
        else:
            self.log.info("v1 POST request: {idkey} messageID {mid} was already in pendingRetrievals", idkey=jsonBody['identityKey'][0:10], mid=messageID[0:10])
            self.log.error("v1 POST request: message was already in pendingRetrievals")
        
        self.log.info("v1 POST request: {idkey} put message {mid} for recipient {recip}", idkey=jsonBody['identityKey'][0:10], mid=messageID[0:10], recip=recipientKeyId[0:10])
        self.log.warn("v1 POST request: put message for recipient")

        request.setResponseCode(200)

        response = {"messageID" : messageID, "response" : ""}
        recipientToken = self.db.get(recipientKeyId + "_token")
        if recipientToken == None:
            self.log.info("v1 POST request: {idkey} could not find token for recipient {recip}", idkey=jsonBody['identityKey'][0:10], recip=recipientKeyId[0:10])
            self.log.error("v1 POST request: could not find token for recipient")
            response["response"] = "200: Success, but no GCM notification (Error 1)"
        else:
            r = GCMPinger.Notify(recipientToken, jsonBody['identityKey'])
            if r.status_code != requests.codes.ok:
                self.log.info("v1 POST request: {idkey} error posting GCM notification for recipient {recip}: {txt}", idkey=jsonBody['identityKey'][0:10], recip=recipientKeyId[0:10], txt=str(r.text))
                self.log.error("v1 POST request: error posting GCM notification: {txt}", txt=str(r.text))
                response["response"] = "200: Success, but no GCM notification (Error 2)"
            else:
                response["response"] = "200: Success"
        return json.dumps(response)

class V1StatusHandler(resource.Resource):
    def __init__(self, db, log):
        self.db = db
        self.log = log
        resource.Resource.__init__(self)
    def render_POST(self, request):
        ccIDHex = request.transport.getPeerCertificate().digest("SHA256").replace(":", "")
        self.log.info("v1 Status request by Client Cert {ccid}", ccid=ccIDHex[0:10])
        self.log.warn("v1 Status request")
        submittedData = request.content.read()
        try:
            jsonBody = json.loads(submittedData)
            validateRequest(self.log, ccIDHex, jsonBody, True)
        except:
            tb = sys.exc_info()
            self.log.error("v1 Status request: Problem handling request. body={data} errortype={t} error={e}", data=submittedData, t=tb[0], e=base64.b64encode(traceback.format_exc()))
            request.setResponseCode(500)
            return "500: Error with request"

        index = self.db.get(jsonBody['identityKey'])
        if index == None:
            self.log.info("v1 Status request: {idkey} not registered", idkey=jsonBody['identityKey'][0:10])
            self.log.error("v1 Status request: not registered")
            request.setResponseCode(406)
            return "406: Register First"
        
        #Look up the MessageIDs
        response = {}
        ids = base64.b64decode(jsonBody['data']).split(", ")
        for i in ids:
            if not i:
                continue
            if self.db.get(i + "_msg"):
                response[i] = "Present"
            else:
                response[i] = "Missing"
        
        self.log.info("v1 Status request: {idkey} successful poll status request {cnt} messages", idkey=jsonBody['identityKey'][0:10], cnt=len(response))
        self.log.warn("v1 Status request: successful poll status request {cnt} messages", cnt=len(response))
        request.setResponseCode(200)
        return json.dumps(response)
