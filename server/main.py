#!/usr/bin/env python

import os, sys, pickle, hashlib, binascii, rocksdb, ConfigParser, json, requests
from twisted.internet import reactor
from twisted.python.filepath import FilePath
from twisted.web import server, resource, http
from twisted.logger import Logger, textFileLogObserver, globalLogPublisher, FilteringLogObserver, LogLevelFilterPredicate, LogLevel
from util import *
from v1api import *

class Database:
    def __init__(self):
        self.db = rocksdb.DB("showme.db", rocksdb.Options(create_if_missing=True))
    def get(self, key):
        return self.db.get(key)
    def put(self, key, val):
        return self.db.put(key, val)
    def delete(self, key):
        return self.db.delete(key)

class RootAppResource(resource.Resource):
    def __init__(self, db, log):
        resource.Resource.__init__(self)

        v1 = V1API()
        self.putChild('v1', v1)
        v1.putChild('register', V1RegisterHandler(db, log))
        v1.putChild('token', V1TokenHandler(db, log))
        v1.putChild('deregister', V1DeregisterHandler(db, log))
        v1.putChild('poll', V1PollHandler(db, log))
        v1.putChild('get', V1GetHandler(db, log))
        v1.putChild('delete', V1DeleteHandler(db, log))
        v1.putChild('put', V1PostHandler(db, log))  
        v1.putChild('status', V1StatusHandler(db, log))
        
class RootBugResource(resource.Resource):
    def __init__(self, db, log):
        resource.Resource.__init__(self)
        v1 = V1API()
        self.putChild('v1', v1)
        v1.putChild('bugreport', V1BugHandler(db, log))  

rocksDatabase = None
if __name__ == "__main__":
    import signal
    def on_exit(sig, func=None):
        global rocksDatabase
        del rocksDatabase
        reactor.stop()
    signal.signal(signal.SIGTERM, on_exit)
    signal.signal(signal.SIGINT, on_exit)
        

    config = ConfigParser.ConfigParser()
    config.read('config')

    GCMPinger.API_KEY = config.get('GCM', 'key')
    GCMPinger.DESTINATION = config.get('GCM', 'endpoint')

    BugReporter.DESTINATION = config.get('Bugs', 'endpoint')
    BugReporter.USERNAME = config.get('Bugs', 'username')
    BugReporter.PASSWORD = config.get('Bugs', 'password')


    log = Logger()
    globalLogPublisher.addObserver(FilteringLogObserver(textFileLogObserver(open(config.get('Logging', 'logfile'), "a")), 
        [LogLevelFilterPredicate(LogLevel.levelWithName(config.get('Logging', 'level')))]))


    rocksDatabase = Database()


    import OpenSSL
    from twisted.internet import ssl

    appSSLCertData = "".join(open(config.get('Server', 'appsslcert')).readlines())
    appSSLCert = ssl.PrivateCertificate.loadPEM(appSSLCertData)

    bugSSLCertData = "".join(open(config.get('Server', 'bugsslcert')).readlines())
    bugSSLCert = ssl.PrivateCertificate.loadPEM(bugSSLCertData)    

    sslCiphers = ssl.AcceptableCiphers.fromOpenSSLCipherString(config.get('Server', 'ciphersuites'))
    dhParams = ssl.DiffieHellmanParameters.fromFile(FilePath(config.get('Server', 'dhparams')))
    appSSLConfig = ssl.CertificateOptions(
        enableSessions = True,
        enableSessionTickets = True,

        privateKey = appSSLCert.privateKey.original,
        certificate = appSSLCert.original,

        enableSingleUseKeys = True,
        acceptableCiphers = sslCiphers,
        dhParameters = dhParams
        )
    bugsSSLConfig = ssl.CertificateOptions(
        enableSessions = True,
        enableSessionTickets = True,

        privateKey = bugSSLCert.privateKey.original,
        certificate = bugSSLCert.original,

        enableSingleUseKeys = True,
        acceptableCiphers = sslCiphers,
        dhParameters = dhParams
        )
    
    """
    pyOpenSSL as API doesn't support tls-unique channel binding or 
    accepting any client certificate. Supports client certs, but  
    must chain to root specify.
    
    Reaching into objects, can trick into accepting any client 
    certificate. Will achieve channel binding
    """
    
    #Cause underlying object to cache
    throwaway = appSSLConfig.getContext()
    def _verifyCallback(conn, cert, errno, depth, preverify_ok):
        return True #We accept any client certificate
    #Now edit context
    requireCert = OpenSSL.SSL.VERIFY_PEER | OpenSSL.SSL.VERIFY_FAIL_IF_NO_PEER_CERT
    appSSLConfig._context.set_verify(requireCert, _verifyCallback)
    
    #Now proceed
    reactor.listenSSL(config.getint('Server', 'appport'), server.Site(RootAppResource(rocksDatabase, log)), appSSLConfig)
    reactor.listenSSL(config.getint('Server', 'bugport'), server.Site(RootBugResource(rocksDatabase, log)), bugsSSLConfig)
    reactor.run()
