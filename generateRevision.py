#!/usr/bin/python

#generateRevision.py
#Usage:
#  >generateRevision.py {revision_no} {revision_id}
#Updates the Revision.REVISION_NO and Revision.REVISION_ID for the CloudStackAndroidClient
#with the pasted.  This script must be placed
#Generates the Revision.java file embedded with the passed in revision_no and revision_id values.
#This script must be run from the base directory of the trunk/branch to work.
#This script should be automatically hooked up to the Bazaar pre_commit hook with 
#pre_commit_hook.py so it can automatically get the newest revision_no/revision_id
#information and feed it to this script.

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
    print "  >generateRevision.py {revision_no} {revision_id} "
    sys.exit(2)

#path of Revision.java file
revisionFilePath = "CloudStackAndroidClient/src/com/creationline/cloudstack/utils/Revision.java"

#create new revision no/id declarations with latest revision info
updatedRevnoLine = "	public static final int REVISION_NO = "+str(revno)+";\n"
updatedRevidLine = "    public static final String REVISION_ID = \""+str(revid)+"\";\n"

#static template of Version class this script will generate
revisionClassTemplate_1stHalf = ('package com.creationline.cloudstack.utils;\n'
                                 '///This Revision.java files is not checked into Bazaar.\n'
                                 '///It is preferable to have this class in source control as well,\n'
                                 '///but currently there does not seem to be a way update\n'
                                 '///REVISION_NO and REVISION_ID automatically upon commit _and_\n'
                                 '///sync the newest values to bzr at the same time (this is an\n'
                                 '///artifact of the way bzr\'s pre_commit and start_commit hooks work.\n'
                                 '///We can use pre_commit to get the next revno/revid and write it to\n'
                                 '///a file, but pre_commit does not allow changing of the contents of\n'
                                 '///the commit.  start_commit can be used to change the contents of\n'
                                 '///the commit before it is actually done (though cannot add new changes\n'
                                 '///to the commit itself apparently?), but it does not have access\n'
                                 '///to the next revno/revid that will be assigned once the commit\n'
                                 '///actually goes through.  The end result of this is that the\n'
                                 '///revision recorded in Revision.java will always be one revision\n'
                                 '////behind the tip of the repository).\n'
                                 '///\n'
                                 '///Therefore, we will use the generateRevision.py script to\n'
                                 '///automatically generate this Revision class upon pre_commit\n'
                                 '///with the latest revision information embedded (the code for\n'
                                 '///this file will also be saved inside generateRevision.py,\n'
                                 '///which is under source control).  We will use this revision\n'
                                 '///info as the developer-use version tracking information.\n'
                                 '///\n'
                                 '///Version.java, provided separately, provides a manual\n'
                                 '///major/minor version number that can be arbitrarily edited by\n'
                                 '///hand for end-user display purposes.  This file is sync-ed to bzr.\n'
                                 '\n'
                                 'public class Revision {\n'
                                )
revisionClassTemplate_2ndHalf = ('\n'
                                 '    @Override\n'
                                 '	public String toString() {\n'
                                 '		return asString();\n'
                                 '	}\n'
                                 '\n'
                                 '    public static String asString() {\n'
                                 '    	return REVISION_NO+" ["+REVISION_ID+"]";\n'
                                 '    }\n'
                                 '}\n'
                                )


#overwrite revision file with new data
print "##Generating Revision.java:\n"
with open(revisionFilePath, "w") as revisionFile:
    print revisionClassTemplate_1stHalf
    print updatedRevnoLine
    print updatedRevidLine
    print revisionClassTemplate_2ndHalf
    revisionFile.write(revisionClassTemplate_1stHalf)
    revisionFile.write(updatedRevnoLine)
    revisionFile.write(updatedRevidLine)
    revisionFile.write(revisionClassTemplate_2ndHalf)
revisionFile.closed
