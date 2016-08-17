import os, requests, json, binascii, pickle, sys, hashlib

def asciihex_to_bytes(keyid): 
    return binascii.unhexlify(keyid.replace(":", ""))
def bytes_to_asciihex(b): 
    return binascii.hexlify(b).upper()

class GCMPinger:
    API_KEY = ""
    DESTINATION = ""

    @staticmethod
    def Notify(recipientToken, senderKey):
        headers = {'Authorization' : 'key=' + GCMPinger.API_KEY}
        payload = {'to' : recipientToken}
        return requests.post(GCMPinger.DESTINATION, headers=headers, json=payload)
        
class BugReporter:
    DESTINATION = ""
    USERNAME = ""
    PASSWORD = ""

    @staticmethod
    def Forward(uuid, payload):
        return requests.put(BugReporter.DESTINATION + uuid, data=payload, auth=(BugReporter.USERNAME, BugReporter.PASSWORD))
    
    
    
