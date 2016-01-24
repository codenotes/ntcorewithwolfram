// wstp_test1.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"

#include "wstp.h"
extern int addtwo(int i, int j);

void examineTheLinks(WSENV env)
{
	WSLINK *theLinks;
	int length;
	int apiResult;

	apiResult = WSGetLinksFromEnvironment(env, &theLinks, &length);
	if (!apiResult)
	{ /* Unable to retrieve the list of open links */
	}

	/* ... */

	WSReleaseLinksFromEnvironment(env, theLinks, length);
}

int addtwo(int i, int j)
{
	WSEvaluateString(stdlink, "booger=66");// "Print[\"negative\"]");
	return i + j;
	

	



}



int main(int argc, char* argv[])
{
	return WSMain(argc, argv);
}

