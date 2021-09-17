package com.hty.dlna;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class DLNAClient {

    String url="", scheme="", IP="", port="", description="", friendlyName="", responseData="";
    List<Map> list_service = new ArrayList<>();

    public DLNAClient(String s) {
        for (String l: s.split("\r\n")) {
            if (l.toUpperCase().startsWith("LOCATION:")) {
                url = l.substring(9).trim();
                Log.e(Thread.currentThread().getStackTrace()[2] + "", url);
                scheme = url.substring(0, url.indexOf("://") + 3);
                l = url.substring(url.indexOf("://") + 3);
                Log.e(Thread.currentThread().getStackTrace()[2] + "", l);
                IP = l.substring(0, l.indexOf(":"));
                port = l.substring(l.indexOf(":") + 1, l.indexOf("/"));
                description = l.substring(l.indexOf("/") + 1);
                Log.e(Thread.currentThread().getStackTrace()[2] + "", scheme + " , " + IP + " , " + port + " , " + description);
                getDescription(url);
            }
        }

    }

    void getDescription(String surl) {
        try {
            URL url = new URL(surl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            InputStream IS = conn.getInputStream();
            InputStreamReader ISR = new InputStreamReader(IS);
            BufferedReader BR = new BufferedReader(ISR);
            String inputLine;
            while ((inputLine = BR.readLine()) != null) {
                responseData += inputLine + "\n";
            }
            //Log.e(Thread.currentThread().getStackTrace()[2] + "", "responseText:\n" + responseData);
            DocumentBuilderFactory DBF = DocumentBuilderFactory.newInstance();
            DocumentBuilder DB = DBF.newDocumentBuilder();
            Document doc = DB.parse(new ByteArrayInputStream(responseData.getBytes()));
            friendlyName = doc.getElementsByTagName("friendlyName").item(0).getFirstChild().getNodeValue();
            Log.e(Thread.currentThread().getStackTrace()[2] + "", friendlyName);
            NodeList NL = doc.getElementsByTagName("service");
            for (int i=0; i<NL.getLength(); i++) {
                NodeList NL1 = NL.item(i).getChildNodes();
                Map map = new HashMap();
                for (int j=0; j<NL1.getLength(); j++) {
                    Node node = NL1.item(j);
                    if (node.getFirstChild() != null) {
                        map.put(node.getNodeName(), node.getFirstChild().getNodeValue());
                        //Log.e(Thread.currentThread().getStackTrace()[2] + "", map.toString());
                    }
                }
                list_service.add(map);
            }
            //Log.e(Thread.currentThread().getStackTrace()[2] + "", list_service.toString());
        } catch (Exception e){
            Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
        }
    }

    String uploadFileToPlay(String controlURL, String UrlToPlay) {
        String XML = "<?xml version=\"1.0\"?>\n"
                +"<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
                +"<SOAP-ENV:Body>\n"
                +"<u:SetAVTransportURI xmlns:u=\"urn:schemas-upnp-org:service:AVTransport:1\">\n"
                +"<InstanceID>0</InstanceID>\n";
        XML += "<CurrentURI>" + UrlToPlay + "</CurrentURI>\n";
        XML += "<CurrentURIMetaData></CurrentURIMetaData>\n";
        XML += "</u:SetAVTransportURI>\n";
        XML += "</SOAP-ENV:Body>\n"
                +"</SOAP-ENV:Envelope>\n";
        String surl = scheme + IP + ":" + port;
        if (!controlURL.startsWith("/"))
            surl += "/";
        surl += controlURL;
        Log.e(Thread.currentThread().getStackTrace()[2] + "", surl);
        return post(surl, XML);
    }

    String post(String surl, String s) {
        String result = "";
        PrintWriter out = null;
        BufferedReader in = null;
        try {
            URL url = new URL(surl);
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
            conn.setRequestProperty("SOAPAction", "urn:schemas-upnp-org:service:AVTransport:1#SetAVTransportURI");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(s);
            out.flush();
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line + "\n";
            }
        } catch (Exception e) {
            return e.toString();
        } finally {
            try {
                if (out != null){
                    out.close();
                }
                if(in != null){
                    in.close();
                }
            } catch (Exception e) {
                return e.toString();
            }
        }
        return result;
    }

}