/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.aft.dme2.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.att.aft.dme2.api.DME2Client;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.handler.FileDownloadHandler;
import com.att.aft.dme2.request.DME2Payload;
import com.att.aft.dme2.request.DME2TextPayload;
import com.att.aft.dme2.request.FilePayload;
import com.att.aft.dme2.request.HttpRequest.RequestBuilder;
import com.att.aft.dme2.request.Request;
import com.att.aft.dme2.test.DME2BaseTestCase;
import com.google.common.collect.Lists;

public class TestDME2Exchange extends DME2BaseTestCase {
	String textFile = "src/test/etc/rinfo.txt";
	String textFile1 = "src/test/etc/rinfo1.txt";
	String binaryFile = "src/test/etc/CSPConfigureWebApp.jar";
	String binaryFile1 = "src/test/etc/lrm-api.jar";
	String invalidFile = "src/test/etc/rinfo10.txt";
	// String binaryFile = "C:\\temp/heap_7624_1.bin";

	@Before
	public void setUp() {
		super.setUp();

	}

	@After
	public void tearDown() {
		super.tearDown();
	}


	@Test
	public void testCheckIfFailOverRequired() throws Exception {
		DME2Manager mgr = null;
		String service = "service=com.att.aft.dme2.TestPayloadAsStream/version=1.0.0/envContext=DEV/routeOffer=D1";

		// System.setProperty("AFT_DME2_PF_SERVICE_NAME",
		// "com.att.aft.TestDME2Exchange_TextFilePayload");

		try {
			DME2Configuration config = new DME2Configuration("com.att.aft.dme2.TestPayloadAsStream");
			mgr = new DME2Manager("com.att.aft.dme2.TestPayloadAsStream", config);
			mgr.bindServiceListener(service, new EchoFileServlet());

			Thread.sleep(1000);

			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.TestPayloadAsStream/version=1.0.0/envContext=DEV/routeOffer=D1";

			Request request = new RequestBuilder(new URI(uriStr))
					.withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false)
					.withLookupURL(uriStr).build();

			DME2Client sender = new DME2Client(mgr, request);
			DME2Payload payload = new FilePayload(textFile, true, false);
			String reply = (String) sender.sendAndWait(payload);
			System.out.println("REPLY = " + reply);

			assertTrue(reply != null);

			assertTrue(reply.contains("Uploaded Filename: "));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE");
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");

			try {
				mgr.unbindServiceListener(service);
			} catch (Exception e) {
			}
			mgr.shutdown();
		}
	}

	@Test
	public void testTextFileAsPayload() throws Exception {
		DME2Manager mgr = null;
		String service = "service=com.att.aft.dme2.TestPayloadAsStream/version=1.0.0/envContext=DEV/routeOffer=D1";

		// System.setProperty("AFT_DME2_PF_SERVICE_NAME",
		// "com.att.aft.TestDME2Exchange_TextFilePayload");

		try {
			DME2Configuration config = new DME2Configuration("com.att.aft.dme2.TestPayloadAsStream");
			mgr = new DME2Manager("com.att.aft.dme2.TestPayloadAsStream", config);
			mgr.bindServiceListener(service, new EchoFileServlet());

			Thread.sleep(1000);

			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.TestPayloadAsStream/version=1.0.0/envContext=DEV/routeOffer=D1";

			Request request = new RequestBuilder(new URI(uriStr))
					.withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false)
					.withLookupURL(uriStr).build();

			DME2Client sender = new DME2Client(mgr, request);
			DME2Payload payload = new FilePayload(textFile, true, false);
			String reply = (String) sender.sendAndWait(payload);
			System.out.println("REPLY = " + reply);

			assertTrue(reply != null);

			assertTrue(reply.contains("Uploaded Filename: "));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE");
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");

			try {
				mgr.unbindServiceListener(service);
			} catch (Exception e) {
			}
			mgr.shutdown();
		}
	}

	@Test
	public void testBinFileAsPayload() throws Exception {
		DME2Manager mgr = null;
		String service = "/service=com.att.aft.dme2.TestPayloadAsBytes/version=1.0.0/envContext=DEV/routeOffer=D1";
		// System.setProperty("AFT_DME2_PF_SERVICE_NAME",
		// "com.att.aft.TestDME2Exchange_BinFilePayload");

		try {
			DME2Configuration config = new DME2Configuration("com.att.aft.dme2.TestPayloadAsBytes");
			mgr = new DME2Manager("com.att.aft.dme2.TestPayloadAsBytes", config);
			mgr.bindServiceListener(service, new EchoFileServlet());

			Thread.sleep(1000);

			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.TestPayloadAsBytes/version=1.0.0/envContext=DEV/routeOffer=D1";
			Request request = new RequestBuilder(new URI(uriStr))
					.withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false)
					.withLookupURL(uriStr).build();

			DME2Client sender = new DME2Client(mgr, request);
			DME2Payload payload = new FilePayload(binaryFile, true, true);

			String reply = (String) sender.sendAndWait(payload);
			System.out.println("REPLY = " + reply);

			assertTrue(reply != null);
			assertTrue(reply.contains("Uploaded Filename: "));

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE");
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");

			try {
				mgr.unbindServiceListener(service);
			} catch (Exception e) {
			}
			mgr.shutdown();
		}
	}

	@Test
	public void testTextFileAsPayloadWithName() throws Exception {
		DME2Manager mgr = null;
		String service = "/service=com.att.aft.dme2.TestTextFileAsPayloadWithName/version=1.0.0/envContext=DEV/routeOffer=D1";

		// System.setProperty("AFT_DME2_PF_SERVICE_NAME",
		// "com.att.aft.TestDME2Exchange_TextFilePayload");
		try {
			DME2Configuration config = new DME2Configuration("com.att.aft.dme2.TestTextFileAsPayloadWithName");
			mgr = new DME2Manager("com.att.aft.dme2.TestTextFileAsPayloadWithName", config);
			EchoFileServlet s = new EchoFileServlet();

			mgr.bindServiceListener(service, s);

			Thread.sleep(1000);

			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.TestTextFileAsPayloadWithName/version=1.0.0/envContext=DEV/routeOffer=D1";
			Request request = new RequestBuilder(new URI(uriStr))
					.withHttpMethod("POST").withReadTimeout(15000).withReturnResponseAsBytes(false)
					.withLookupURL(uriStr).build();

			DME2Client sender = new DME2Client(mgr, request);

			DME2Payload payload = new FilePayload(textFile, true, false, "test_path");

			// EchoReplyHandler replyHandler = new EchoReplyHandler();
			// sender.setUploadFileWithMultiPart(textFile, false,"test_path");
			// sender.setMethod("POST");
			String reply = (String) sender.sendAndWait(payload);

			// String reply = replyHandler.getResponse(60000);
			System.out.println("REPLY 1 =" + reply);

			assertTrue(reply != null);
			assertTrue(reply.contains("Uploaded Filename: "));
			// stop server that replied

			String fieldname = reply.substring(reply.indexOf("FieldName:") + "FieldName:".length()).trim();

			assertEquals("test_path", fieldname);

			mgr.getServer().stop();

		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e == null);
		} finally {
			System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE");
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
			mgr.unbindServiceListener(service);
			mgr.shutdown();
		}
	}

	@Test
	public void testInvalidFileAsPayloadWithName() throws Exception {
		DME2Manager mgr = null;
		String service = "/service=com.att.aft.dme2.TestInvalidFileAsPayloadWithName/version=1.0.0/envContext=DEV/routeOffer=D1";

		// System.setProperty("AFT_DME2_PF_SERVICE_NAME",
		// "com.att.aft.TestDME2Exchange_TextFilePayload");
		try {
			DME2Configuration config = new DME2Configuration("com.att.aft.dme2.TestInvalidFileAsPayloadWithName");
			mgr = new DME2Manager("com.att.aft.dme2.TestInvalidFileAsPayloadWithName", config);

			EchoFileServlet s = new EchoFileServlet();

			mgr.bindServiceListener(service, s);

			Thread.sleep(1000);

			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.TestInvalidFileAsPayloadWithName/version=1.0.0/envContext=DEV/routeOffer=D1";
			Request request = new RequestBuilder(new URI(uriStr))
					.withHttpMethod("POST").withReadTimeout(15000).withReturnResponseAsBytes(false)
					.withLookupURL(uriStr).build();

			DME2Client sender = new DME2Client(mgr, request);
			DME2Payload payLoad = new FilePayload(invalidFile, true, false, "test_path");

			try {
				sender.sendAndWait(payLoad);
			} catch (Exception e) {
				e.printStackTrace();
				String exMessage = e.getMessage();
				boolean result = exMessage.contains("filepath not found");
				assertTrue(result);
			}
			// String reply = replyHandler.getResponse(60000);

			mgr.getServer().stop();

		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e == null);
		} finally {
			System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE");
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
			mgr.unbindServiceListener(service);
			mgr.shutdown();
		}
	}

	@Test
	public void testBinFileAsPayloadWithName() throws Exception {
		DME2Manager mgr = null;
		String service = "/service=com.att.aft.dme2.TestPayloadAsBytesWithName/version=1.0.0/envContext=DEV/routeOffer=D1";
		// System.setProperty("AFT_DME2_PF_SERVICE_NAME",
		// "com.att.aft.TestDME2Exchange_BinFilePayload");
		try {
			DME2Configuration config = new DME2Configuration("com.att.aft.dme2.TestPayloadAsBytesWithName");
			mgr = new DME2Manager("com.att.aft.dme2.TestPayloadAsBytesWithName", config);

			EchoFileServlet s = new EchoFileServlet();

			mgr.bindServiceListener(service, s);

			Thread.sleep(1000);

			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.TestPayloadAsBytesWithName/version=1.0.0/envContext=DEV/routeOffer=D1";
			Request request = new RequestBuilder(new URI(uriStr))
					.withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false)
					.withLookupURL(uriStr).build();

			DME2Client sender = new DME2Client(mgr, request);
			// DME2FileUploadInfo fileInfo=new DME2FileUploadInfo();
			// fileInfo.setFilepath(binaryFile);
			// fileInfo.setFileName("test_path");
			// sender.setUploadFileWithMultiPart(fileInfo, true);
			// sender.setMethod("POST");

			DME2Payload payload = new FilePayload(binaryFile, true, true, "test_path");
			String reply = (String) sender.sendAndWait(payload);

			// String reply = replyHandler.getResponse(60000);
			System.out.println("REPLY 1 =" + reply);

			assertTrue(reply != null);
			assertTrue(reply.contains("Uploaded Filename: "));
			String fieldname = reply.substring(reply.indexOf("FieldName:") + "FieldName:".length()).trim();
			assertEquals("test_path", fieldname);
			// stop server that replied
			mgr.getServer().stop();

		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e == null);
		} finally {
			System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE");
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
			mgr.unbindServiceListener(service);
			mgr.shutdown();
		}
	}

	@Test
	public void testMultiTextFilesAsPayloadWithSameName() throws Exception {
		DME2Manager mgr = null;
		String service = "/service=com.att.aft.dme2.TestMultiTextFilesWithSameName/version=1.0.0/envContext=DEV/routeOffer=D1";
		// System.setProperty("AFT_DME2_PF_SERVICE_NAME",
		// "com.att.aft.TestDME2Exchange_BinFilePayload");
		try {
			DME2Configuration config = new DME2Configuration("com.att.aft.dme2.TestMultiTextFilesWithSameName");
			mgr = new DME2Manager("com.att.aft.dme2.TestMultiTextFilesWithSameName", config);

			EchoFileServlet s = new EchoFileServlet();
			mgr.bindServiceListener(service, s);

			Thread.sleep(1000);

			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.TestMultiTextFilesWithSameName/version=1.0.0/envContext=DEV/routeOffer=D1";
			Request request = new RequestBuilder(new URI(uriStr))
					.withHttpMethod("POST").withReadTimeout(35000).withReturnResponseAsBytes(false)
					.withLookupURL(uriStr).build();

			DME2Client sender = new DME2Client(mgr, request);

			/*
			 * List<DME2FileUploadInfo> filesUploadList=new
			 * ArrayList<DME2FileUploadInfo>(); DME2FileUploadInfo fileinfo1=new
			 * DME2FileUploadInfo(); fileinfo1.setFilepath(textFile);
			 * fileinfo1.setFileName("test_path");
			 * filesUploadList.add(fileinfo1);
			 *
			 * DME2FileUploadInfo fileinfo2=new DME2FileUploadInfo();
			 * fileinfo2.setFilepath(textFile1);
			 * fileinfo2.setFileName("test_path");
			 * filesUploadList.add(fileinfo2);
			 */

			List<String> filesUploadList = Lists.newArrayList(textFile, textFile1);
			DME2Payload payload = new FilePayload(filesUploadList, true, false, "test_path");

			// sender.setUploadFileWithMultiPart(filesUploadList, true);
			// sender.setMethod("POST");
			String reply = (String) sender.sendAndWait(payload);

			// String reply = replyHandler.getResponse(60000);
			System.out.println("REPLY 1 =" + reply);

			assertTrue(reply != null);
			assertTrue(reply.contains("Uploaded Filename: "));
			String fieldnameRemain = reply.substring(reply.indexOf("FieldName:") + "FieldName:".length()).trim();
			String fieldName1 = fieldnameRemain.substring(0, fieldnameRemain.indexOf("Uploaded Filename: "));
			String fieldName2 = fieldnameRemain.substring(fieldnameRemain.indexOf("FieldName:") + "FieldName:".length())
					.trim();

			assertEquals(fieldName1.trim(), "test_path");
			assertEquals(fieldName2.trim(), "test_path");

			// assertEquals("test_path_files", fieldname);
			// stop server that replied
			mgr.getServer().stop();

		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e == null);
		} finally {
			System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE");
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
			mgr.unbindServiceListener(service);
			mgr.shutdown();
		}
	}

	@Test
	public void testMultiTextFilesAsPayloadWithDiffName() throws Exception {
		DME2Manager mgr = null;
		String service = "/service=com.att.aft.dme2.TestMultiTextFilesWithDiffName/version=1.0.0/envContext=DEV/routeOffer=D1";

		// System.setProperty("AFT_DME2_PF_SERVICE_NAME",
		// "com.att.aft.TestDME2Exchange_BinFilePayload");
		try {
			DME2Configuration config = new DME2Configuration("com.att.aft.dme2.TestMultiTextFilesWithDiffName");
			mgr = new DME2Manager("com.att.aft.dme2.TestMultiTextFilesWithDiffName", config);

			EchoFileServlet s = new EchoFileServlet();
			mgr.bindServiceListener(service, s);

			Thread.sleep(1000);

			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.TestMultiTextFilesWithDiffName/version=1.0.0/envContext=DEV/routeOffer=D1";
			Request request = new RequestBuilder(new URI(uriStr))
					.withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false)
					.withLookupURL(uriStr).build();

			DME2Client sender = new DME2Client(mgr, request);

			/*
			 * List<DME2FileUploadInfo> filesUploadList=new
			 * ArrayList<DME2FileUploadInfo>(); DME2FileUploadInfo fileinfo1=new
			 * DME2FileUploadInfo(); fileinfo1.setFilepath(textFile);
			 * fileinfo1.setFileName("test_path_1");
			 * filesUploadList.add(fileinfo1);
			 *
			 * DME2FileUploadInfo fileinfo2=new DME2FileUploadInfo();
			 * fileinfo2.setFilepath(textFile1);
			 * fileinfo2.setFileName("test_path_2");
			 * filesUploadList.add(fileinfo2);
			 */

			List<String> filesUploadList = Lists.newArrayList(textFile, textFile1);
			DME2Payload payload = new FilePayload(filesUploadList, true, true, "test_path_2");

			// sender.setUploadFileWithMultiPart(filesUploadList, true);
			// sender.setMethod("POST");
			String reply = (String) sender.sendAndWait(payload);

			// String reply = replyHandler.getResponse(60000);
			System.out.println("REPLY 1 =" + reply);

			assertTrue(reply != null);
			assertTrue(reply.contains("Uploaded Filename: "));
			String fieldnameRemain = reply.substring(reply.indexOf("FieldName:") + "FieldName:".length()).trim();
			String fieldName1 = fieldnameRemain.substring(0, fieldnameRemain.indexOf("Uploaded Filename: "));
			String fieldName2 = fieldnameRemain.substring(fieldnameRemain.indexOf("FieldName:") + "FieldName:".length())
					.trim();

			assertEquals("test_path_2", fieldName1.trim());
			assertEquals("test_path_2", fieldName2.trim());

			// assertEquals("test_path_files", fieldname);
			// stop server that replied
			mgr.getServer().stop();

		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e == null);
		} finally {
			System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE");
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
			mgr.unbindServiceListener(service);
			mgr.shutdown();
		}
	}

	@Test
	public void testMultiBinFilesAsPayloadWithSameName() throws Exception {
		DME2Manager mgr = null;
		String service = "/service=com.att.aft.dme2.TestMultiBinFilesWithSameName/version=1.0.0/envContext=DEV/routeOffer=D1";
		// System.setProperty("AFT_DME2_PF_SERVICE_NAME",
		// "com.att.aft.TestDME2Exchange_BinFilePayload");
		try {
			DME2Configuration config = new DME2Configuration("com.att.aft.dme2.TestMultiBinFilesWithSameName");
			mgr = new DME2Manager("com.att.aft.dme2.TestMultiBinFilesWithSameName", config);

			EchoFileServlet s = new EchoFileServlet();
			mgr.bindServiceListener(service, s);

			Thread.sleep(1000);

			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.TestMultiBinFilesWithSameName/version=1.0.0/envContext=DEV/routeOffer=D1";
			Request request = new RequestBuilder(new URI(uriStr))
					.withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false)
					.withLookupURL(uriStr).build();

			DME2Client sender = new DME2Client(mgr, request);

			/*
			 * List<DME2FileUploadInfo> filesUploadList=new
			 * ArrayList<DME2FileUploadInfo>(); DME2FileUploadInfo fileinfo1=new
			 * DME2FileUploadInfo(); fileinfo1.setFilepath(binaryFile);
			 * fileinfo1.setFileName("test_path");
			 * filesUploadList.add(fileinfo1);
			 *
			 * DME2FileUploadInfo fileinfo2=new DME2FileUploadInfo();
			 * fileinfo2.setFilepath(binaryFile1);
			 * fileinfo2.setFileName("test_path");
			 * filesUploadList.add(fileinfo2);
			 */

			List<String> filesUploadList = Lists.newArrayList(binaryFile, binaryFile1);
			DME2Payload payload = new FilePayload(filesUploadList, true, true, "test_path");

			// sender.setUploadFileWithMultiPart(filesUploadList, true);
			// sender.setMethod("POST");
			String reply = (String) sender.sendAndWait(payload);

			// String reply = replyHandler.getResponse(60000);
			System.out.println("REPLY 1 =" + reply);

			assertTrue(reply != null);
			assertTrue(reply.contains("Uploaded Filename: "));
			String fieldnameRemain = reply.substring(reply.indexOf("FieldName:") + "FieldName:".length()).trim();
			String fieldName1 = fieldnameRemain.substring(0, fieldnameRemain.indexOf("Uploaded Filename: "));
			String fieldName2 = fieldnameRemain.substring(fieldnameRemain.indexOf("FieldName:") + "FieldName:".length())
					.trim();

			assertEquals("test_path", fieldName1.trim());
			assertEquals("test_path", fieldName2.trim());

			// assertEquals("test_path_files", fieldname);
			// stop server that replied
			mgr.getServer().stop();

		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e == null);
		} finally {
			System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE");
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
			mgr.unbindServiceListener(service);
			mgr.shutdown();
		}
	}

	@Test
	public void testMultiBinFilesAsPayloadWithDiffName() throws Exception {
		DME2Manager mgr = null;
		String service = "/service=com.att.aft.dme2.TestMultiBinFilesWithDiffName/version=1.0.0/envContext=DEV/routeOffer=D1";
		// System.setProperty("AFT_DME2_PF_SERVICE_NAME",
		// "com.att.aft.TestDME2Exchange_BinFilePayload");
		try {
			DME2Configuration config = new DME2Configuration("com.att.aft.dme2.TestMultiBinFilesWithDiffName");
			mgr = new DME2Manager("com.att.aft.dme2.TestMultiBinFilesWithDiffName", config);

			EchoFileServlet s = new EchoFileServlet();
			mgr.bindServiceListener(service, s);

			Thread.sleep(1000);

			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.TestMultiBinFilesWithDiffName/version=1.0.0/envContext=DEV/routeOffer=D1";
			Request request = new RequestBuilder(new URI(uriStr))
					.withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false)
					.withLookupURL(uriStr).build();

			DME2Client sender = new DME2Client(mgr, request);

			/*
			 * List<DME2FileUploadInfo> filesUploadList=new
			 * ArrayList<DME2FileUploadInfo>(); DME2FileUploadInfo fileinfo1=new
			 * DME2FileUploadInfo(); fileinfo1.setFilepath(binaryFile);
			 * fileinfo1.setFileName("bin_path_1");
			 * filesUploadList.add(fileinfo1);
			 *
			 * DME2FileUploadInfo fileinfo2=new DME2FileUploadInfo();
			 * fileinfo2.setFilepath(binaryFile1);
			 * fileinfo2.setFileName("bin_path_2");
			 * filesUploadList.add(fileinfo2);
			 */

			List<String> filesUploadList = Lists.newArrayList(binaryFile, binaryFile1);
			DME2Payload payload = new FilePayload(filesUploadList, true, true, "bin_path_2");

			// sender.setUploadFileWithMultiPart(filesUploadList, true);
			// sender.setMethod("POST");

			String reply = (String) sender.sendAndWait(payload);

			// String reply = replyHandler.getResponse(60000);
			System.out.println("REPLY 1 =" + reply);

			assertTrue(reply != null);
			assertTrue(reply.contains("Uploaded Filename: "));
			String fieldnameRemain = reply.substring(reply.indexOf("FieldName:") + "FieldName:".length()).trim();
			String fieldName1 = fieldnameRemain.substring(0, fieldnameRemain.indexOf("Uploaded Filename: "));
			String fieldName2 = fieldnameRemain.substring(fieldnameRemain.indexOf("FieldName:") + "FieldName:".length())
					.trim();

			assertEquals("bin_path_2", fieldName1.trim());
			assertEquals("bin_path_2", fieldName2.trim());

			// assertEquals("test_path_files", fieldname);
			// stop server that replied
			mgr.getServer().stop();

		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e == null);
		} finally {
			System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE");
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");
			mgr.unbindServiceListener(service);
			mgr.shutdown();
		}
	}

	@Test
	public void testInvalidFileAsPayload() throws Exception {
		DME2Manager mgr = null;
		String service = "/service=com.att.aft.dme2.TestInvalidFileAsPayload/version=1.0.0/envContext=DEV/routeOffer=D1";
		// System.setProperty("AFT_DME2_PF_SERVICE_NAME",
		// "com.att.aft.TestDME2Exchange_InvalidFilePayload");

		try {
			DME2Configuration config = new DME2Configuration("com.att.aft.dme2.TestInvalidFileAsPayload");
			mgr = new DME2Manager("com.att.aft.dme2.TestInvalidFileAsPayload", config);
			mgr.bindServiceListener(service, new EchoFileServlet());

			Thread.sleep(1000);

			String uriStr = "http://DME2RESOLVE:8767/service=com.att.aft.dme2.TestInvalidFileAsPayload/version=1.0.0/envContext=DEV/routeOffer=D1";
			Request request = new RequestBuilder(new URI(uriStr))
					.withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false)
					.withLookupURL(uriStr).build();

			DME2Client sender = new DME2Client(mgr, request);

			// sender.setUploadFileWithMultiPart("abc.zip", true);

			List<String> filePaths = Lists.newArrayList();
			filePaths.add("abc.zip");

			DME2Payload payload = new FilePayload(filePaths, true, true);
			// sender.setMethod("POST");

			sender.sendAndWait(payload);

			fail("Error occured in the test case. Excpecting AFT-DME2-0720 error to be thrown. File abc.zip does not exist.");
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e.getMessage().contains("AFT-DME2-0720"));
		} finally {
			System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE");
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");

			try {
				mgr.unbindServiceListener(service);
			} catch (Exception e) {
			}
			mgr.shutdown();
		}
	}

	@Test
	public void testFileAsPayload() throws Exception {
		DME2Manager mgr = null;
		String service = "/service=com.att.aft.dme2.TestFileAsPayload/version=1.0.0/envContext=DEV/routeOffer=D1";
		// System.setProperty("AFT_DME2_PF_SERVICE_NAME",
		// "com.att.aft.TestDME2Exchange_FilePayload");

		try {
			DME2Configuration config = new DME2Configuration("com.att.aft.dme2.TestFileAsPayload");
			mgr = new DME2Manager("com.att.aft.dme2.TestFileAsPayload", config);
			mgr.bindServiceListener(service, new EchoFileServlet());

			Thread.sleep(1000);

			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.TestFileAsPayload/version=1.0.0/envContext=DEV/routeOffer=D1";
			Request request = new RequestBuilder(new URI(uriStr))
					.withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false)
					.withLookupURL(uriStr).build();

			DME2Client sender = new DME2Client(mgr, request);
			// sender.setUploadFile(textFile);
			// sender.setMethod("POST");

			List<String> filePaths = Lists.newArrayList();
			filePaths.add(textFile);
			DME2Payload payload = new FilePayload(filePaths, false, false);

			String reply = (String) sender.sendAndWait(payload);
			System.out.println("REPLY = " + reply);

			assertTrue(reply != null);

			/*
			 * Apache fileupload API doesn't support this and hence expected
			 * output is no file uploaded. The file upload works, but not in
			 * multipart format
			 */
			assertTrue(reply.contains("No file uploaded"));

		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e.getMessage().contains("AFT-DME2-0715"));
		} finally {
			System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE");
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");

			try {
				mgr.unbindServiceListener(service);
			} catch (Exception e) {
			}
			mgr.shutdown();
		}
	}

	@Test
	public void testFileDownload() throws Exception {
		DME2Manager mgr = null;
		String service = "/service=com.att.aft.dme2.TestFileDownload/version=1.0.0/envContext=DEV/routeOffer=D1";
		// System.setProperty("AFT_DME2_PF_SERVICE_NAME",
		// "com.att.aft.TestDME2Exchange_FileDownload");

		try {
			DME2Configuration config = new DME2Configuration("com.att.aft.dme2.TestFileDownload");
			mgr = new DME2Manager("com.att.aft.dme2.TestFileDownload", config);
			mgr.bindServiceListener(service, new EchoFileServlet());

			Thread.sleep(1000);

			String uriStr = "http://DME2RESOLVE/service=com.att.aft.dme2.TestFileDownload/version=1.0.0/envContext=DEV/routeOffer=D1";
			Request request = new RequestBuilder(new URI(uriStr))
					.withHttpMethod("POST").withReadTimeout(30000).withReturnResponseAsBytes(false)
					.withLookupURL(uriStr).withHeader("testReturnFile", "true").build();

			String filePath = System.getProperty("java.io.tmpdir");

			FileDownloadHandler handler = new FileDownloadHandler(filePath + File.separator + "CSPConfigureWebApp.jar",
					service);


			DME2Client sender = new DME2Client(mgr, request);
			// sender.setPayload("test");
			// sender.setMethod("POST");
			sender.setResponseHandlers(handler);
			// sender.setHeaders(headers);
			sender.send(new DME2TextPayload("test"));

			String response = handler.getResponse(15000);
			assertEquals("true", response);
		} catch (Exception e) {
			e.printStackTrace();
			assertEquals(e.getMessage(), "AFT-DME2-0715");
		} finally {
			System.clearProperty("AFT_DME2_SKIP_RELOAD_EPS_ALL_STALE");
			System.clearProperty("AFT_DME2_PF_SERVICE_NAME");

			try {
				mgr.unbindServiceListener(service);
			} catch (Exception e) {
			}
			mgr.shutdown();
		}
	}

}
