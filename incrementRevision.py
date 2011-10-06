#!/usr/bin/python

#incrementRevision.py
#Usage:
#  >incrementRevision.py {revision_no} {revision_id}
#Increments the Revision.REVISION_NO and Revision.REVISION_ID for the CloudStackAndroidClient
#based off of the latest Bazaar branch revision information.  This script must be placed
#in the base directory of the trunk/branch to work.
#This script should be automatically hooked up to the Bazaar pre_commit hook with 
#pre_commit_hook.py so it can automatically update CloudStackAndroidClient's version
#info to the latest upon each commit.

import re
import sys
import getopt

#parse next revno & revid from args
try:
    opts, args = getopt.getopt(sys.argv[1:], [], []) 
    revno = args[0]
    revid = args[1]
except IndexError, err:
    print "Error: Required arguments missing!"
    print "Usage:"
    print "  >incrementVersion.py {revision_no} {revision_id} "
    sys.exit(2)

#path of Revision.java file
revisionFilePath = "CloudStackAndroidClient/src/com/creationline/cloudstack/util/Revision.java"

#create new revision no/id declarations with latest revision info
updatedRevnoLine = "	public static final int REVISION_NO = "+str(revno)+";"
updatedRevidLine = "    public static final String REVISION_ID = \""+str(revid)+"\";"
warningComment = "  //this must be on its own line (updated by incrementRevision.py automatically)\n"

#open and read in file with version data
with open(revisionFilePath, "r") as revisionFile:
    originalData = revisionFile.readlines()
revisionFile.closed

#replace the relevant lines with new revision lines
newData = []
for line in originalData:
    if re.match("(.*)REVISION_NO =(.*);", line):
	line = updatedRevnoLine+warningComment
    if re.match("(.*)REVISION_ID =(.*);", line):
	line = updatedRevidLine+warningComment
    newData.append(line)

#overwrite version file with new data
print "##Updated Revision.java:\n"
with open(revisionFilePath, "w") as revisionFile:
    for line in newData:
        print line
        revisionFile.write(line)
revisionFile.closed
