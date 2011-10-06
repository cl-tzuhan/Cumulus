#!/usr/bin/python

#incrementVersion.py
#Increments the Version.BUILD_NO for the CloudStackAndroidClient by 1.
#This script must be placed in the base directory of the trunk/branch to work.
#This script should be automatically hooked up to the Bazaar start_commit hook with
#start_commit_hook.py so it can automatically update CloudStackAndroidClient's version
#info upon each commit.

import re

#path of Version.java file
versionFilePath = "CloudStackAndroidClient/src/com/creationline/cloudstack/util/Version.java"

buildnoDeclaration = "    public static final int BUILD_NO = "
warningComment = "  //this must be on its own line (updated by incrementVersion.py automatically)\n"

#open and read in file with version data
with open(versionFilePath, "r") as versionFile:
    originalData = versionFile.readlines()
versionFile.closed

#replace the relevant lines with new revision lines
newData = []
for line in originalData:
    if re.match("(.*)BUILD_NO =(.*);", line):
	#tease out the actual build number (assumed to be 7th token on that line)
	#as an int and build a new buildno declaration that's incremented by one
	splitLine = line.split()
	buildno = int(splitLine[6].strip(";"))
	newBuildnoLine = buildnoDeclaration+str(buildno+1)+";"+warningComment
	line = newBuildnoLine
    newData.append(line)

#overwrite version file with new data
print "##Updated Version.java:\n"
with open(versionFilePath, "w") as versionFile:
    for line in newData:
        print line
        versionFile.write(line)
versionFile.closed





#copy new BUILD_NO to Revision.java
revisionFilePath = "CloudStackAndroidClient/src/com/creationline/cloudstack/util/Revision.java"

with open(revisionFilePath, "r") as revisionFile:
    originalData = revisionFile.readlines()
revisionFile.closed

newData = []
for line in originalData:
    if re.match("(.*)BUILD_NO =(.*);", line):
        line = newBuildnoLine
    newData.append(line)

print "##Updated Revision.java:\n"
with open(revisionFilePath, "w") as revisionFile:
    for line in newData:
        print line
        revisionFile.write(line)
revisionFile.closed

