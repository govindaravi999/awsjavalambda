package com.rgr.checkin.seatreaccom;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/***
 * Created: 06/01/2019<br>
 * Copyright: Copyright 2019<br>
 * Company: Sabre Holdings Corporation<br>
 *
 * @author Govinda Ravipati - SG0304569<br>
 */

public class LambdaMethodHandler implements RequestHandler<S3Event, Response> {


    public Response handleRequest(S3Event event, Context ctx) {

        boolean enablePayLoadLogs = Boolean.valueOf(System.getenv("PAY_LOAD_LOGS_ENABLE"));
        AmazonS3Client s3Client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain());
        S3EventNotification.S3EventNotificationRecord record = event.getRecords().get(0);
        String s3Key = record.getS3().getObject().getKey();
        String s3Bucket = record.getS3().getBucket().getName();
        ctx.getLogger().log("Bucket Name is " + s3Bucket);
        ctx.getLogger().log("File Path is " + s3Key);
        S3Object s3object = s3Client.getObject(new GetObjectRequest(s3Bucket, s3Key));
        DefaultHttpClient httpClient = new DefaultHttpClient();
        String url = getServiceUrl(s3Key, ctx);
        Response response = new Response();
        if (url == null) {
            ctx.getLogger().log("end point not found for key::" + s3Key);
            response.setMessage("end point not found for key::" + s3Key);
            throw new RuntimeException("end point not found for" + s3Key);
        }
        ctx.getLogger().log("Call SeatReaccom Service End Point::" + url);

        try {

            StringBuilder jsonContent = getJsonData(s3object, ctx);
            if (enablePayLoadLogs) {
                ctx.getLogger().log("json data::" + jsonContent.toString());
            }
            HttpPost postRequest = new HttpPost(url);
            StringEntity payLoad = new StringEntity(jsonContent.toString());
            payLoad.setContentType("application/json");
            postRequest.setEntity(payLoad);
            HttpResponse httpResponse = httpClient.execute(postRequest);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            ctx.getLogger().log("StatusCode::" + statusCode);
            if (statusCode == 200) {
                ctx.getLogger().log(url + " invoke Successful");
                String responseData = convertStreamToString(httpResponse.getEntity().getContent(), ctx);
                response.setMessage(responseData);
                if (enablePayLoadLogs) {
                    ctx.getLogger().log("engine response:::" + responseData);
                }

            } else {
                throw new RuntimeException("Failed with HTTP error code : " + statusCode);
            }
        } catch (Exception ex) {
            response.setMessage("Exception Details:" + ex.getMessage());
        } finally {
            httpClient.getConnectionManager().shutdown();
        }

        return response;
    }

    private StringBuilder getJsonData(S3Object s3object, Context ctx) {

        StringBuilder builder = new StringBuilder();
        try {

            BufferedReader reader = new BufferedReader(new InputStreamReader(s3object.getObjectContent()));
            String jsonContent = null;
            while ((jsonContent = reader.readLine()) != null) {
                builder.append(jsonContent);

            }
        } catch (Exception exception) {

            ctx.getLogger().log("Exception during reading jsocn context" + exception.getMessage());
        }


        return builder;
    }

    private String convertStreamToString(InputStream is, Context ctx) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            ctx.getLogger().log("Exception during reading response context" + e.getMessage());
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
                ctx.getLogger().log("Exception during reading response context" + e.getMessage());
            }
        }
        return sb.toString();
    }

    private String getServiceUrl(String s3Key, Context ctx) {

        ctx.getLogger().log("s3key" + s3Key);
      /**  if (s3Key.contains("RuleSet")) {
            ctx.getLogger().log("ruleset found");
            return System.getenv("REACCOM_SERVICE_RULES_ENDPOINT");
        } else if (s3Key.contains("PadisGroup")) {
            ctx.getLogger().log("PadisGroup found");
            return System.getenv("REACCOM_SERVICE_PADISGROUP_ENDPOINT");
        }**/
        return System.getenv("REACCOM_SERVICE_RULES_ENDPOINT");

    }


}