import java.net.*;
import java.io.*;

public class Index{
  public void doPost(Request request, Socket clientSocket) throws IOException{	
		PageServer.log("Post call recieved with content " +request.getContent());
		PageServer.defaultRequestProcess(request, clientSocket);
 }

 public void doGet(Request request, Socket clientSocket) throws IOException{	
		PageServer.defaultRequestProcess(request, clientSocket);
 }
}