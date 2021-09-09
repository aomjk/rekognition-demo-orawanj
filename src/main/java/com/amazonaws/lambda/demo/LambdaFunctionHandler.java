package com.amazonaws.lambda.demo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
//import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
/*import com.amazonaws.services.rekognition.model.GetLabelDetectionRequest;
import com.amazonaws.services.rekognition.model.GetLabelDetectionResult;
import com.amazonaws.services.rekognition.model.LabelDetection;
import com.amazonaws.services.rekognition.model.LabelDetectionSortBy;*/
import com.amazonaws.services.rekognition.model.Image;
//import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Label;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
//import org.elasticsearch.client.RestHighLevelClient;

import com.amazonaws.services.rekognition.model.S3Object;

//import com.amazonaws.regions.*;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
//import com.amazonaws.auth.AWSRequestSigningApacheInterceptor;

import org.apache.http.HttpEntity;
/*import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;*/
//import com.amazonaws.http.AWSRequestSigningApacheInterceptor;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LambdaFunctionHandler implements RequestHandler<S3Event, String> {

    private AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();
    //private DynamoDB dynamoDb;
    //private String DYNAMODB_TABLE_NAME = "Person";
   // private Regions REGION = Regions.US_EAST_1;
    

  //  private static String payload = "{ \"type\": \"s3\", \"settings\": { \"bucket\": \"aom-s3-01\", \"region\": \"us-east-1\", \"role_arn\": \"arn:aws:iam::478263352179:role/service-role/S3ToRekognition-role-ytbwemb5\" } }";
  //  private static String snapshotPath = "/_snapshot/my-snapshot-repo";

    private static String sampleDocument = "{" + "\"title\":\"Walk the Line\"," + "\"director\":\"James Mangold\"," + "\"year\":\"2005\"}";
    private static String indexingPath = "/photo_index/_doc";
    
    private static String serviceName = "es";
    private static String region = "us-east-1";
    private static String aesEndpoint = "https://<<es-endpoint>>";

    static final AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();


    public LambdaFunctionHandler() {}

    // Test purpose only.
    LambdaFunctionHandler(AmazonS3 s3) {
        this.s3 = s3;
    }

    @Override
    public String handleRequest(S3Event event, Context context){
        context.getLogger().log("Received event: " + event);

        // Get the object from the event and show its content type
        String bucket = event.getRecords().get(0).getS3().getBucket().getName();
        String key = event.getRecords().get(0).getS3().getObject().getKey();
        String temp = "{"+"\"bucketName\":"+"\""+bucket+"\","+"\"key\":"+"\""+key+"\",";
        
        System.out.println("******key : " + key);
        try {
           // S3Object response = s3.getObject(new GetObjectRequest(bucket, key));
           // String contentType = response.getObjectMetadata().getContentType();
           // context.getLogger().log("CONTENT TYPE: " + contentType);
            
            //AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();
            
            AmazonRekognition rekognitionClient =
            		 AmazonRekognitionClientBuilder.defaultClient();
            		 DetectLabelsRequest request = new DetectLabelsRequest()
            		 .withImage(new Image()
            		 .withS3Object(new S3Object()
            		 .withName(key).withBucket(bucket)))
            		 .withMaxLabels(10)
            		 .withMinConfidence(75F); 
            
           
			 DetectLabelsResult result = rekognitionClient.detectLabels(request);
			 List <Label> labels = result.getLabels();
			 System.out.println("Detected labels for " + "aws.png");
			 
			 if(labels!=null) {
				 int i = labels.size();
				 System.out.println("label size : "+i);
				 
				 if(i>0) {
					 int j = 1;
					
					 for (Label label: labels) {
						
						 System.out.println(label.getName() + ": " +
								 label.getConfidence().toString());

						 if(j<i) {
							 temp = temp + "\""+label.getName()+"\""+":"+"\""+label.getConfidence().toString()+"\",";
							 j++;
						 }else if(j==i){
							 temp = temp + "\""+label.getName()+"\""+":"+"\""+label.getConfidence().toString()+"\"";
						 }
					 }
					 
					 temp = temp +"}";
				 
				 }
			 }
			// RestClient esClient = esClient(serviceName, region);
			 
			 System.out.println("temp = "+temp);
			 ////////////// Elasticsearch /////////////////
			/* AWS4Signer signer = new AWS4Signer();
		        signer.setServiceName("es");
		        signer.setRegionName("eu-west-1");
		        HttpRequestInterceptor interceptor = new AWSRequestSigningApacheInterceptor("es", signer, new DefaultAWSChain());
		        String endpoint = "https://vpc-aom-o247nl76zvbhkm3y6465kwkarq.us-east-1.es.amazonaws.com" ;
		        RestHighLevelClient restHighLevelClient = new RestHighLevelClient(RestClient.builder(HttpHost.create(endpoint)).setHttpClientConfigCallback(hacb -> hacb.addInterceptorLast(interceptor)));
		     
		        */
			 
			 // Register a snapshot repository
			 try {
			 
			 RestClient esClient = esClient(serviceName, region);
			
			 
		        //HttpEntity entity = new NStringEntity(payload, ContentType.APPLICATION_JSON);
		        /*Request req = new Request("PUT", snapshotPath);
		       
		        req.setEntity(entity);
		        // request.addParameter(name, value); // optional parameters
		        Response response = esClient.performRequest(req);
		        System.out.println(response.toString());*/

		        // Index a document
			 	//HttpEntity entity = new NStringEntity(sampleDocument, ContentType.APPLICATION_JSON);
			 	HttpEntity entity = new NStringEntity(temp, ContentType.APPLICATION_JSON);
		        String id = "1";
		        Request req = new Request("PUT", indexingPath + "/" + id);
		        req.setEntity(entity);

		        // Using a String instead of an HttpEntity sets Content-Type to application/json automatically.
		        // request.setJsonEntity(sampleDocument);
		        Response response = esClient.performRequest(req);
		        response = esClient.performRequest(req);
		        System.out.println(response.toString());
			 }catch(IOException e) {
				 e.printStackTrace();
				 //throws e;
			 }
			 
			 //////////////////////////////////////////////
            
          
            String contentType = "";
           
            return contentType;
            
        
        } catch(Exception e) {
        	 e.printStackTrace();
             //  context.getLogger().log(String.format(
             //      "Error getting object %s from bucket %s. Make sure they exist and"
             //      + " your bucket is in the same region as this function.", key, bucket));
               throw e;
        }
    }
 // Adds the interceptor to the ES REST client
    public static RestClient esClient(String serviceName, String region) {
    	
    	AWS4Signer signer = new AWS4Signer();
        signer.setServiceName(serviceName);
        signer.setRegionName(region);
        HttpRequestInterceptor interceptor = new AWSRequestSigningApacheInterceptor(serviceName, signer, credentialsProvider);
        return RestClient.builder(HttpHost.create(aesEndpoint)).setHttpClientConfigCallback(hacb -> hacb.addInterceptorLast(interceptor)).build();
    
  	
    }
}