#!/usr/bin/env python

import os, base64, json, time, subprocess, binascii, requests, pickle
import axolotl_curve25519

ROOT_URL        = "https://127.0.0.1:8082"
REGISTER_URL    = ROOT_URL + "/v1/register"
TOKEN_URL    	= ROOT_URL + "/v1/token"
DEREGISTER_URL  = ROOT_URL + "/v1/deregister"
POLL_URL        = ROOT_URL + "/v1/poll"
GET_URL         = ROOT_URL + "/v1/get"
DELETE_URL      = ROOT_URL + "/v1/delete"
PUT_URL         = ROOT_URL + "/v1/put"
STATUS_URL      = ROOT_URL + "/v1/status"

OPENSSL_CNF = \
"""
[ req ]
distinguished_name = req_distinguished_name
prompt = no

[ req_distinguished_name ]
C = SU
ST                     = Test State or Province
L                      = Test Locality
O                      = Organization Name
OU                     = Organizational Unit Name
CN                     = Common Name
emailAddress           = test@email.address
"""

cert1 = None
id1 = None
cert2 = None
id2 = None

jsonHeader = {'Content-Type': 'application/json'}
    
def wrap_data_for_recipient(data, recipkey):
    return recipkey[1] + pickle.dumps(data)

def unwrap_data(data):
    return pickle.loads(data)

class URLException(Exception):
    def __init__(self, req):
        s = "\n"
        s += "\tRequest:" + req.url + "\n"
        s += "\tResponse Code:" + str(req.status_code) + "\n"
        super(URLException, self).__init__(s)
        
def test_setup():
    global cert1, cert2, id1, id2
    try:
        os.mkdir(".tmp")
    except:
        pass
    
    ocnf = open(os.path.join(".tmp", "openssl.cnf"), "w")
    ocnf.write(OPENSSL_CNF)
    ocnf.close()
    
    cert_file_path1 = os.path.join(".tmp", "cert1.cer")
    key_file_path1 = os.path.join(".tmp", "cert1.key")
    
    cert_file_path2 = os.path.join(".tmp", "cert2.cer")
    key_file_path2 = os.path.join(".tmp", "cert2.key")
    
    os.system("openssl genrsa > " + key_file_path1 + " 2>/dev/null")
    os.system("openssl genrsa > " + key_file_path2 + " 2>/dev/null")
    
    os.system("openssl req -new -x509 -key " + key_file_path1 + \
              " -out " + cert_file_path1 + \
              " -config " + os.path.join(".tmp", "openssl.cnf"))
    
    os.system("openssl req -new -x509 -key " + key_file_path2 + \
              " -out " + cert_file_path2 + \
              " -config " + os.path.join(".tmp", "openssl.cnf"))
    
    cert1_fingerprint = subprocess.check_output("openssl x509 -in " + \
        cert_file_path1 + " -noout -sha256 -fingerprint | " + \
        " cut -f 2 -d '=' | tr -d ':'", shell=True).strip()

    cert2_fingerprint = subprocess.check_output("openssl x509 -in " + \
        cert_file_path2 + " -noout -sha256 -fingerprint | " + \
        " cut -f 2 -d '=' | tr -d ':'", shell=True).strip()

    cert1 = (cert_file_path1, key_file_path1, cert1_fingerprint)
    cert2 = (cert_file_path2, key_file_path2, cert2_fingerprint)
    
    rnd1 = os.urandom(32)
    privkey1 = axolotl_curve25519.generatePrivateKey(rnd1)
    pubkey1 = axolotl_curve25519.generatePublicKey(privkey1)

    rnd2 = os.urandom(32)
    privkey2 = axolotl_curve25519.generatePrivateKey(rnd2)
    pubkey2 = axolotl_curve25519.generatePublicKey(privkey2)

    id1 = (privkey1, pubkey1)
    id2 = (privkey2, pubkey2)
    
    if not os.path.isfile(cert1[0]) or \
       not os.path.isfile(cert1[1]) or \
       not os.path.isfile(cert2[0]) or \
       not os.path.isfile(cert2[1]):
        raise Exception("Could not setup client certificates")
    
def test_teardown():
    global cert1, cert2, id1, id2
    
    try:
        os.remove(cert1[0])
    except:
        pass
        
    try:
        os.remove(cert1[1])
    except:
        pass
        
    try:
        os.remove(cert2[0])
    except:
        pass
        
    try:
        os.remove(cert2[1])
    except:
        pass
        
    try:
        os.remove(os.path.join(".tmp", "openssl.cnf"))
    except:
        pass
    
    try:
        os.rmdir(".tmp")
    except:
        pass
    
def signCert(cert, idkey, photoData=None):
    payload = {}
    payload['identityKey'] = binascii.hexlify(idkey[1]).upper()
    payload['timestamp'] = int(time.time())
    payload['signatureVersion'] = 1
    if photoData != None:
       payload['data'] = base64.b64encode(photoData)
    
    #TODO: What the correct way to canonicalize this data for hashing/signing?
    sigData = chr(1) + str(payload['timestamp']) + binascii.unhexlify(cert[2])
    
    randm64 = os.urandom(64)
    signature = axolotl_curve25519.calculateSignature(randm64, idkey[0], sigData)
    payload['signature'] = base64.b64encode(signature)
    return payload

def test_errors():
    global cert1, cert2, id1, id2, jsonHeader

    tampereddata = signCert(cert1, id1)
    tampereddata['signature'] = '8L2XdRY0e2ZZIg9vvjTZZvHEByY78HRZEXwYNDmjs7FA+m9m1uXetvMTY+Tva23vD2CpUfW80c9V1pC0k/p2DA'
    r = requests.post(REGISTER_URL, verify=False, cert=cert1, json=tampereddata)
    if r.status_code != 500: raise URLException(r)
    

    tampereddata = signCert(cert1, id1)
    r = requests.post(REGISTER_URL, verify=False, cert=cert2, json=tampereddata)
    if r.status_code != 500: raise URLException(r)


    tampereddata = signCert(cert2, id2)
    del tampereddata['signature']
    r = requests.post(REGISTER_URL, verify=False, cert=cert2, json=tampereddata)
    if r.status_code != 500: raise URLException(r)


    tampereddata = signCert(cert2, id2)
    del tampereddata['identityKey']
    r = requests.post(REGISTER_URL, verify=False, cert=cert2, json=tampereddata)
    if r.status_code != 500: raise URLException(r)


    tampereddata = signCert(cert2, id2)
    del tampereddata['timestamp']
    r = requests.post(REGISTER_URL, verify=False, cert=cert2, json=tampereddata)
    if r.status_code != 500: raise URLException(r)


    tampereddata = signCert(cert2, id2)
    del tampereddata['signatureVersion']
    r = requests.post(REGISTER_URL, verify=False, cert=cert2, json=tampereddata)
    if r.status_code != 500: raise URLException(r)


    payload = {}
    payload['identityKey'] = binascii.hexlify(id2[1]).upper()
    payload['timestamp'] = int(time.time() - 100 - (60 * 60 * 24 * 3))
    payload['signatureVersion'] = 2
    
    #TODO: What the correct way to canonicalize this data for hashing/signing?
    sigData = chr(2) + str(payload['timestamp']) + binascii.unhexlify(cert2[2])
    
    randm64 = os.urandom(64)
    signature = axolotl_curve25519.calculateSignature(randm64, id2[0], sigData)
    payload['signature'] = base64.b64encode(signature)

    r = requests.post(REGISTER_URL, verify=False, cert=cert2, json=payload)
    if r.status_code != 500: raise URLException(r)



def test_register():
    #Register #1
    r = requests.post(REGISTER_URL, verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 201: raise URLException(r)
    
    #Re-Register #1
    r = requests.post(REGISTER_URL, verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 200: raise URLException(r)

    #Token #1
    r = requests.post(TOKEN_URL + "/thisismytoken", verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 200: raise URLException(r)
    
    #Attempt Poll on #2
    r = requests.post(POLL_URL, verify=False, cert=cert2, json=signCert(cert2, id2))
    if r.status_code != 406: raise URLException(r)
    
    #Attempt Status on #2
    r = requests.post(STATUS_URL, verify=False, cert=cert2, json=signCert(cert2, id2, "hello, world"))
    if r.status_code != 406: raise URLException(r)
    
    #Attempt Get on #2
    r = requests.post(GET_URL + "/1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef", verify=False, cert=cert2, json=signCert(cert2, id2))
    if r.status_code != 406: raise URLException(r)

    #Attempt Token on #2
    r = requests.post(TOKEN_URL + "/1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef", verify=False, cert=cert2, json=signCert(cert2, id2))
    if r.status_code != 406: raise URLException(r)
    
    #Attempt Delete on #2
    r = requests.post(DELETE_URL + "/1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef", verify=False, cert=cert2, json=signCert(cert2, id2))
    if r.status_code != 406: raise URLException(r)
    
    #Attempt Put for #2 by 1
    photoData = wrap_data_for_recipient(set(['012345', '6789']), id2)
    r = requests.post(PUT_URL, verify=False, cert=cert1, json=signCert(cert1, id1, photoData))
    if r.status_code != 404: raise URLException(r)
    
    #Register #2
    r = requests.post(REGISTER_URL, verify=False, cert=cert2, json=signCert(cert2, id2))
    if r.status_code != 201: raise URLURLException(r)
    
    #Re-Register #2
    r = requests.post(REGISTER_URL, verify=False, cert=cert2, json=signCert(cert2, id2))
    if r.status_code != 200: raise URLException(r)

def test_get_put1():
    global cert1, cert2, id1, id2
    #Poll #1
    r = requests.post(POLL_URL, verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 200: raise URLException(r)
    
    #Put for #1 by #2
    data1 = set(['012345', '6789'])
    data = wrap_data_for_recipient(data1, id1)
    r = requests.post(PUT_URL, verify=False, cert=cert2, json=signCert(cert2, id2, data))
    if r.status_code != 200: raise URLException(r)
    
    #Poll #1
    r = requests.post(POLL_URL, verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 200: raise URLException(r)
    mids = json.loads(r.content)
    if len(mids) != 1: raise Exception("Poll response not what expected")

    #Status #1
    r = requests.post(STATUS_URL, verify=False, cert=cert1, json=signCert(cert1, id1, str(mids[0]) + ", abcde"))
    if r.status_code != 200: raise URLException(r)
    if json.loads(r.content)[mids[0]] != "Present": raise Exception("Returned data not expected value")
    if json.loads(r.content)["abcde"] != "Missing": raise Exception("Returned data not expected value")

    #Get #1
    r = requests.post(GET_URL + "/" + mids[0], verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 200: raise URLException(r)
    if pickle.loads(r.content) != data1: raise Exception("Returned data not expected value")

    #Delete #1
    r = requests.post(DELETE_URL + "/" + mids[0], verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 204: raise URLException(r)
    
    #Get #1
    r = requests.post(GET_URL + "/" + mids[0], verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 404: raise URLException(r)
    
    #Poll #1
    r = requests.post(POLL_URL, verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 200: raise URLException(r)
    mids = json.loads(r.content)
    if len(mids) != 0: raise Exception("Poll response not what expected")
    
def test_get_put2():
    global cert1, cert2, id1, id2
    #Poll #1
    r = requests.post(POLL_URL, verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 200: raise URLException(r)
    mids = json.loads(r.content)
    if len(mids) != 0: raise Exception("Poll response not what expected")
    
    #Put for #1 by #2
    data1 = set(['012345', '6789'])
    data = wrap_data_for_recipient(data1, id1)
    r = requests.post(PUT_URL, verify=False, cert=cert2, json=signCert(cert2, id2, data))
    if r.status_code != 200: raise URLException(r)
    
    #Put for #1 by #2
    data2 = set(['012345', 'ABCDEF'])
    data = wrap_data_for_recipient(data2, id1)
    r = requests.post(PUT_URL, verify=False, cert=cert2, json=signCert(cert2, id2, data))
    if r.status_code != 200: raise URLException(r)
    
    #Poll #1
    r = requests.post(POLL_URL, verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 200: raise URLException(r)
    mids = json.loads(r.content)
    if len(mids) != 2: raise Exception("Poll response not what expected")
    
    #Get #1
    r = requests.post(GET_URL + "/" + mids[0], verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 200: raise URLException(r)
    if pickle.loads(r.content) != data1 and pickle.loads(r.content) != data2: raise Exception("Returned data not expected value")

    #Get #1
    r = requests.post(GET_URL + "/" + mids[1], verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 200: raise URLException(r)
    if pickle.loads(r.content) != data1 and pickle.loads(r.content) != data2: raise Exception("Returned data not expected value")
    
    #Delete #1
    r = requests.post(DELETE_URL + "/" + mids[0], verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 204: raise URLException(r)
    
    #Delete #1
    r = requests.post(DELETE_URL + "/" + mids[1], verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 204: raise URLException(r)
    
    #Delete #1 Again
    r = requests.post(DELETE_URL + "/" + mids[0], verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 204: raise URLException(r)

    #Get #1
    r = requests.post(GET_URL + "/" + mids[0], verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 404: raise URLException(r)
    
    #Get #1
    r = requests.post(GET_URL + "/" + mids[1], verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 404: raise URLException(r)
    
    #Poll #1
    r = requests.post(POLL_URL, verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 200: raise URLException(r)
    mids = json.loads(r.content)
    if len(mids) != 0: raise Exception("Poll response not what expected")

def test_get_put3():
    global cert1, cert2, id1, id2
    #Get #2
    r = requests.post(GET_URL + "/1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef", verify=False, cert=cert2, json=signCert(cert2, id2))
    if r.status_code != 404: raise URLException(r)
    
    #Put for #2 by #2
    data1 = set(['012345', '6789'])
    data = wrap_data_for_recipient(data1, id2)
    r = requests.post(PUT_URL, verify=False, cert=cert2, json=signCert(cert2, id2, data))
    if r.status_code != 500: raise URLException(r)
    
    #Put for #2 by #1
    data1 = set(['012345', '6789'])
    data = wrap_data_for_recipient(data1, id2)
    r = requests.post(PUT_URL, verify=False, cert=cert1, json=signCert(cert1, id1, data))
    if r.status_code != 200: raise URLException(r)
    
    #Put for #2 by #1
    data2 = set(['012345', 'ABCDEF'])
    data = wrap_data_for_recipient(data2, id2)
    r = requests.post(PUT_URL, verify=False, cert=cert1, json=signCert(cert1, id1, data))
    if r.status_code != 200: raise URLException(r)
    
    #Poll #2
    r = requests.post(POLL_URL, verify=False, cert=cert2, json=signCert(cert2, id2))
    if r.status_code != 200: raise URLException(r)
    mids = json.loads(r.content)
    if len(mids) != 2: raise Exception("Poll response " + str(len(mids)) + ", not what expected: 2")
    
    #Get #2
    r = requests.post(GET_URL + "/" + mids[0], verify=False, cert=cert2, json=signCert(cert2, id2))
    if r.status_code != 200: raise URLException(r)
    if pickle.loads(r.content) != data1 and pickle.loads(r.content) != data2: raise Exception("Returned data not expected value")
    
    #Delete #2
    r = requests.post(DELETE_URL + "/" + mids[0], verify=False, cert=cert2, json=signCert(cert2, id2))
    if r.status_code != 204: raise URLException(r)
    
    #Poll #2
    r = requests.post(POLL_URL, verify=False, cert=cert2, json=signCert(cert2, id2))
    if r.status_code != 200: raise URLException(r)
    mids = json.loads(r.content)
    if len(mids) != 1: raise Exception("Poll response not what expected")
    
    #Get #2
    r = requests.post(GET_URL + "/" + mids[0], verify=False, cert=cert2, json=signCert(cert2, id2))
    if r.status_code != 200: raise URLException(r)
    if pickle.loads(r.content) != data1 and pickle.loads(r.content) != data2: raise Exception("Returned data not expected value")
    
    #Delete #2
    r = requests.post(DELETE_URL + "/" + mids[0], verify=False, cert=cert2, json=signCert(cert2, id2))
    if r.status_code != 204: raise URLException(r)
    
    #Get #2
    r = requests.post(GET_URL + "/" + mids[0], verify=False, cert=cert2, json=signCert(cert2, id2))
    if r.status_code != 404: raise URLException(r)
    
    #Get #2
    r = requests.post(GET_URL + "/" + mids[0], verify=False, cert=cert2, json=signCert(cert2, id2))
    if r.status_code != 404: raise URLException(r)
    
    #Poll #2
    r = requests.post(POLL_URL, verify=False, cert=cert2, json=signCert(cert2, id2))
    if r.status_code != 200: raise URLException(r)
    mids = json.loads(r.content)
    if len(mids) != 0: raise Exception("Poll response not what expected")

def test_dup():
    global cert1, cert2, id1, id2
    #Put for #2 by #1
    data1 = set(['012345', '6789'])
    data = wrap_data_for_recipient(data1, id2)
    r = requests.post(PUT_URL, verify=False, cert=cert1, json=signCert(cert1, id1, data))
    if r.status_code != 200: raise URLException(r)
    
    #Poll #2
    r = requests.post(POLL_URL, verify=False, cert=cert2, json=signCert(cert2, id2))
    if r.status_code != 200: raise URLException(r)
    mids = json.loads(r.content)
    if len(mids) != 1: raise Exception("Poll response " + str(len(mids)) + ", not what expected: 1")

    #Put for #2 by #1 Again
    r = requests.post(PUT_URL, verify=False, cert=cert1, json=signCert(cert1, id1, data))
    if r.status_code != 200: raise URLException(r)
    
    #Poll #2
    r = requests.post(POLL_URL, verify=False, cert=cert2, json=signCert(cert2, id2))
    if r.status_code != 200: raise URLException(r)
    mids = json.loads(r.content)
    if len(mids) != 1: raise Exception("Poll response " + str(len(mids)) + ", not what expected: 1")
    
    #Get #2
    r = requests.post(GET_URL + "/" + mids[0], verify=False, cert=cert2, json=signCert(cert2, id2))
    if r.status_code != 200: raise URLException(r)
    if pickle.loads(r.content) != data1: raise Exception("Returned data not expected value")
    
    #Delete #2
    r = requests.post(DELETE_URL + "/" + mids[0], verify=False, cert=cert2, json=signCert(cert2, id2))
    if r.status_code != 204: raise URLException(r)
    
def test_deregister():
    global cert1, cert2, id1, id2
    #Poll #1
    r = requests.post(POLL_URL, verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 200: raise URLException(r)
    mids = json.loads(r.content)
    if len(mids) != 0: raise Exception("Poll response not what expected")
    
    #Deregister #1
    r = requests.post(DEREGISTER_URL, verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 204: raise Exception("Status Code " + str(r.status_code) + " not expected on deregister")
    
    #Re Deregister #1
    r = requests.post(DEREGISTER_URL, verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 200: raise Exception("Status Code " + str(r.status_code) + " not expected on deregister")
    
    #Attempt Poll on #1
    r = requests.post(POLL_URL, verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 406: raise URLException(r)
    
    #Attempt Get on #1
    r = requests.post(GET_URL + "/1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef", verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 406: raise URLException(r)
    
    #Attempt Delete on #1
    r = requests.post(DELETE_URL + "/1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef", verify=False, cert=cert1, json=signCert(cert1, id1))
    if r.status_code != 406: raise URLException(r)
    
    #Attempt Put for #1 by #2
    data = wrap_data_for_recipient(set(['012345', '6789']), id1)  
    r = requests.post(PUT_URL, verify=False, cert=cert2, json=signCert(cert2, id2, data))
    if r.status_code != 404: raise URLException(r)
    
    #Deregister #2
    r = requests.post(DEREGISTER_URL, verify=False, cert=cert2, json=signCert(cert2, id2))
    if r.status_code != 204: raise Exception("Status Code " + str(r.status_code) + " not expected on deregister")

if __name__ == '__main__':
    requests.packages.urllib3.disable_warnings()
    test_setup()
    
    print "Running Tests for certificates & ids:"
    print "\t", cert1[2], " ", binascii.hexlify(id1[1])
    print "\t", cert2[2], " ", binascii.hexlify(id2[1])
    
    test_errors()
    test_register()
    test_get_put1()
    test_get_put2()
    test_get_put3()
    test_dup()
    test_deregister()
    test_teardown()
