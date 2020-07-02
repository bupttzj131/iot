package iot;

import org.apache.xml.security.utils.Base64;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.iot.model.v20180120.PubRequest;
import com.aliyuncs.iot.model.v20180120.PubResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
public class ServerPub {
	public static void main(String[] args) {
		try 
		{ 
			String accessKey = "LTAI4GE1dZD5nCVrofFD7wZG";
			String accessSecret = "oyEGMLE82T6KPIlVUNCXNNkbvhAQfF";
			DefaultProfile.addEndpoint("cn-shanghai", "cn-shanghai", "Iot", "iot.cn-shanghai.aliyuncs.com");
			IClientProfile profile = DefaultProfile.getProfile("cn-shanghai", accessKey, accessSecret);
			DefaultAcsClient client = new DefaultAcsClient(profile); //初始化SDK客户端
			
			PubRequest request = new PubRequest(); 
			request.setProductKey("a1BzNZfN2TX"); 
			request.setMessageContent(Base64.encode("hello world".getBytes())); 
			request.setTopicFullName("/a1BzNZfN2TX/i950Demo/user/get"); 
			request.setQos(0); //目前支持QoS0和QoS1 
		
		   PubResponse response = client.getAcsResponse(request); 
		   System.out.println(response.getSuccess()); 
		   System.out.println(response.getErrorMessage());
		} 
		catch (Exception e)
		{
		   e.printStackTrace();
		}
	}
}
