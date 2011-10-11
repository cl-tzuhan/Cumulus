package com.creationline.cloudstack.engine;

import java.io.InputStream;
import java.io.StringBufferInputStream;

import android.test.AndroidTestCase;

@SuppressWarnings("deprecation")
public class CsRestServiceTest extends AndroidTestCase {

	CsRestService csRestService = null;
	
	@Override
	protected void setUp() throws Exception {
		csRestService = new CsRestService();
		
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {

		super.tearDown();
	}

	public CsRestServiceTest() {
		super();
	}
	
	
	public void testSignRequest() {
		///Tests for a specific request+key signing with pre-determined result
		final String apiKey = "namomNgZ8Qt5DuNFUWf3qpGlQmB4650tY36wFOrhUtrzK13d66qNpttKw52Brj02dbtIHs01y-lCLz1UOzTxVQ";
		final String sortedUrl = "account=thsu-account&apikey=namomngz8qt5dunfuwf3qpglqmb4650ty36wforhutrzk13d66qnpttkw52brj02dbtihs01y-lclz1uoztxvq&command=listvirtualmachines&domainid=2&response=json";
		final String expectedSignedResult = "anoAR%2FAaugrU6uemcuRUw%2Fma0RI%3D";
		
		String signedResult = CsRestService.signRequest(sortedUrl, apiKey);
		//System.out.println("signedResult="+signedResult);
		assertEquals(expectedSignedResult, signedResult);
	}
	
	public void testInputStreamToString() {

		final String sampleText = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ 1234567890 !#$%&\'()-^\\@[;:],./=~|`{+*}<>?";
		InputStream sampleTextStream = new StringBufferInputStream(sampleText);
		StringBuilder result1 = csRestService.inputStreamToString(sampleTextStream);
		assertEquals(sampleText, result1.toString());
		
		final String emptyStr = "";
		InputStream emptyStream = new StringBufferInputStream(emptyStr);
		StringBuilder result2 = csRestService.inputStreamToString(emptyStream);
		assertEquals(emptyStr, result2.toString());
		
		String bigStr = sampleText + sampleText + sampleText + sampleText + sampleText + sampleText + sampleText;
		bigStr = bigStr + bigStr + bigStr + bigStr + bigStr;
		InputStream bigStrStream = new StringBufferInputStream(bigStr);
		StringBuilder result3 = csRestService.inputStreamToString(bigStrStream);
		assertEquals(bigStr, result3.toString());
	}
	
	public void testBuildFinalUrl() {
		//Test for request+key signing with pre-determined result for thsu-account@192.168.3.11:8080
		String host = "http://192.168.3.11:8080/client/api";
		String apiCmd = "command=listVirtualMachines&account=thsu-account&domainid=2";
		String apiKey = "namomNgZ8Qt5DuNFUWf3qpGlQmB4650tY36wFOrhUtrzK13d66qNpttKw52Brj02dbtIHs01y-lCLz1UOzTxVQ";
		String secretKey = "Yt_9ZEIDGlmRIg63MiMatAri-1aRoo4l-82mnbYdR3d8JdG7jvXqrrB5TpmbLZB_8zK_j95VRSQWZwnu0153eQ";
		String expectedFinalUrl = "http://192.168.3.11:8080/client/api?command=listVirtualMachines&account=thsu-account&domainid=2&response=json&apiKey=namomNgZ8Qt5DuNFUWf3qpGlQmB4650tY36wFOrhUtrzK13d66qNpttKw52Brj02dbtIHs01y-lCLz1UOzTxVQ&signature=AZW5TbyF8QY07lPWxk0JZyMwFx0%3D";
		
		String finalizedRequest = CsRestService.buildFinalUrl(host, apiCmd, apiKey, secretKey);
		assertEquals(expectedFinalUrl, finalizedRequest);

		
		//Test for request+key signing with pre-determined result for iizuka1@72.52.126.24
		host = "http://72.52.126.24/client/api";
		apiCmd = "command=listVirtualMachines&account=iizuka1";
		apiKey = "fUFqsJeECZcMawm9q376WKFKdFvd51GLHwgm3d9PD-r3mjNJUaXBYbkKxBoxCdF5EubJ-ypmT8vHihtAm-gZvA";
		secretKey = "Q3s_-gMYzivbaaO9S_2ewdXHSXHvUg6ExP0W2yRWBZxFIbTDIKD3ADk-0NU6qhsD0K31e9Irchh_Z8yuRQTuqQ";
		expectedFinalUrl = "http://72.52.126.24/client/api?command=listVirtualMachines&account=iizuka1&response=json&apiKey=fUFqsJeECZcMawm9q376WKFKdFvd51GLHwgm3d9PD-r3mjNJUaXBYbkKxBoxCdF5EubJ-ypmT8vHihtAm-gZvA&signature=ueOd4fR%2BnPDv8Rcvb5qLNoGl80Y%3D";
		
		finalizedRequest = CsRestService.buildFinalUrl(host, apiCmd, apiKey, secretKey);
		assertEquals(expectedFinalUrl, finalizedRequest);
	}
	
}
