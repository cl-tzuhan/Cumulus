/*******************************************************************************
 * Copyright 2011-2012 Creationline,Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.creationline.zabbix.engine.api;

import org.codehaus.jackson.JsonNode;

import android.test.AndroidTestCase;

public class ZbxApiRequestTest extends AndroidTestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testAuthTokenIsInvalid() {
		{
			final String validReplyBody_invalidAuthToken = "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32602,\"message\":\"Invalid params.\",\"data\":\"Not authorized\"},\"id\":\"23\"}";
			JsonNode rootNode = ZbxApiRequest.parseReplyTextAsJson(validReplyBody_invalidAuthToken);
			assertNotNull("parsing of reply failed when it shouldn't", rootNode);

			final boolean testingValidReplyBody = ZbxApiRequest.authTokenIsInvalid(rootNode);
			assertTrue("This sample response should result in an invalid auth token detection", testingValidReplyBody);
		}

		{
			final String validReplyBody_successfulLogin = "{\"jsonrpc\":\"2.0\",\"result\":\"8b68fc183760ce6677450a609e62e2d8\",\"id\":\"6\"}";
			JsonNode rootNode = ZbxApiRequest.parseReplyTextAsJson(validReplyBody_successfulLogin);
			assertNotNull("parsing of reply failed when it shouldn't", rootNode);
			
			final boolean testingValidReplyBody = ZbxApiRequest.authTokenIsInvalid(rootNode);
			assertFalse("This sample response should not result in an invalid auth token detection", testingValidReplyBody);
		}

		{
			final String invalidReplyBody = "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32602,\"data\":\"Not authorized\"},\"id\":\"23\"}";
			JsonNode rootNode = ZbxApiRequest.parseReplyTextAsJson(invalidReplyBody);
			assertNotNull("parsing of reply failed when it shouldn't", rootNode);
			
			final boolean testingInvalidReplyBody = ZbxApiRequest.authTokenIsInvalid(rootNode);
			assertFalse("This sample response is missing message field, so should not result in invalid auth token detection", testingInvalidReplyBody);
		}
		{
			final String invalidReplyBody = "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32602,\"message\":\"Invalid params.\"},\"id\":\"23\"}";
			JsonNode rootNode = ZbxApiRequest.parseReplyTextAsJson(invalidReplyBody);
			assertNotNull("parsing of reply failed when it shouldn't", rootNode);
			
			final boolean testingInvalidReplyBody = ZbxApiRequest.authTokenIsInvalid(rootNode);
			assertFalse("This sample response is missing data field, so should not result in invalid auth token detection", testingInvalidReplyBody);
		}
		{
			final String invalidReplyBody = "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32602},\"id\":\"23\"}";
			JsonNode rootNode = ZbxApiRequest.parseReplyTextAsJson(invalidReplyBody);
			assertNotNull("parsing of reply failed when it shouldn't", rootNode);
			
			final boolean testingInvalidReplyBody = ZbxApiRequest.authTokenIsInvalid(rootNode);
			assertFalse("This sample response is missing message & data fields, so should not result in invalid auth token detection", testingInvalidReplyBody);
		}
		
		
		
		
	}

}
