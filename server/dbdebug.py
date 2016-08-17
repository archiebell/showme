#!/usr/bin/env python

import pickle, binascii, rocksdb

db = rocksdb.DB("showme.db", rocksdb.Options(create_if_missing=False, ))

it = db.iterkeys()
it.seek_to_first()
for i in it:
        if len(i) == 64:
                print "User", i

		token = db.get(i + "_token")
		if token and token == "thisismytoken":
                        #This was a user from the test harness. Just delete it.
                        db.delete(i)
                        db.delete(i + "_token")
		        for m in msgs:
			        present = db.get(m)
                                if present:
                                        db.delete(m)
                        continue

                if token:
                        print "\t", "Token:", token

                msgs = []
                try:
                        msgs = pickle.loads(db.get(i))
                except:
                        print "\t", "Error unpickling messages len:", len(db.get(i))
                for m in msgs:
                        present = db.get(m + "_msg")
			print "\t", m, "Present:" + "Yes (" + str(len(present)) + ")" if present != None else "No"

        elif len(i) == 70:
                if db.get(i) == "thisismytoken":
                        db.delete(i)
                else:
                        print "Token:", i
        elif len(i) == 68:
                print "Message:", i
        else:
		print len(i), binascii.hexlify(i)           
