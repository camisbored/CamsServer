import java.net.*;
import java.io.*;
public class SecondPage{
 
 public String doPost(Request request, Socket clientSocket) throws IOException{
  	try{
		PageServer.customResponse(clientSocket, "Post data recieved: "+ request.getContent());
 		return "SUCCESS";
	} catch (Exception e){
		return e.getLocalizedMessage();
 	}
 }

 public String doGet(Request request, Socket clientSocket) throws IOException{
  	try{
		PageServer.defaultRequestProcess(request, clientSocket);
 		return "SUCCESS";
	} catch (Exception e){
		return e.getLocalizedMessage();
 	}
 }
}