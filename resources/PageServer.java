import java.io.*;
import java.util.regex.*;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.*;
import javax.tools.ToolProvider;
import java.util.Date;
import java.util.HashMap;

public class PageServer{
	static HashMap<String, String> contentTypeMap = new HashMap<String, String>();
	static String generatedDirectory = System.getProperty("user.dir");
	static String rootDirectory = new File(generatedDirectory).getParent();
	static String resourceDirectory = new File(generatedDirectory).getParent()+File.separator+"resources";
	static {
		contentTypeMap.put("html", "text/html");
		contentTypeMap.put("txt", "text/html");
		contentTypeMap.put("jsp", "text/html");
		contentTypeMap.put("png", "image/png");
		contentTypeMap.put("jpeg", "image/jpeg");
		contentTypeMap.put("ico", "image/x-icon");
		contentTypeMap.put("jpg", "image/*");
	}
   public static void main(String[] args) throws Exception {
    int port = 80;
    if (args.length==1)
    	port = Integer.parseInt(args[0]);
    ServerSocket serverSocket = new ServerSocket(port);
    log("Server launched on port : " + port);

    while (true) {
	Socket clientSocket = serverSocket.accept();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.submit(()->{
	     try{ 
		String firstString = "";
	        log("New Connection from "+ clientSocket.getInetAddress());
	        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	        String s;
	        Request request = new Request();
	        while ((s = in.readLine()) != null) {
			    if (firstString.isEmpty())
			    	firstString = s;
			    if (s.contains("Content-Length:"))
			    	request.setContentLength(Integer.parseInt(s.split(":")[1].trim()));
		        log(s);
		        if (s.isEmpty()) 
		            break;
	        }
	        request.setMethod(firstString.split("\\s+")[0]);
	        if (request.getContentLength() > 0) {
	        	int c = 0;
			StringBuilder postData = new StringBuilder();
	        	for (int i=0; i<request.getContentLength(); i++) {
	        		c = in.read();
	        		postData.append((char) c);
	        	}
	        	request.setContent(postData.toString());
	        }
	        String requestedResource = firstString.split("\\s+")[1];
			if (requestedResource.contains("?")){
				request.setQuery(requestedResource.split("\\?")[1]);
				request.setResourceRequested(requestedResource.split("\\?")[0]);
			}
			else 
				request.setResourceRequested(requestedResource);
			
			if (request.getResourceRequested().equals("/"))
				request.setResourceRequested("index.html");

			if (!request.getResourceRequested().contains("."))
					request.setFileExtension("");
			else {
				request.setFileExtension(request.getResourceRequested().split("\\.")[1]);
				request.setResourceRequested(request.getResourceRequested().split("\\.")[0]);
			}
			if (request.getMethod().equals("GET") || request.getMethod().equals("POST")) {
				String nameForJavaCheck = request.getResourceRequested();
				if (!nameForJavaCheck.equals("index"))
					nameForJavaCheck = nameForJavaCheck.substring(1);
				nameForJavaCheck = nameForJavaCheck.substring(0,1).toUpperCase()+nameForJavaCheck.substring(1);
				if (new File(resourceDirectory +File.separator+nameForJavaCheck+".java").exists()) {
					request.setResourceRequested(nameForJavaCheck);
					String result = compileAndRun(request.getResourceRequested(), request, clientSocket);
					if (null != result && !result.equals("SUCCESS"))
						displayErrorPage("500", result, request, clientSocket);
				}
				else 
					defaultRequestProcess(request, clientSocket);
			}
			else {
				displayErrorPage("405", "Method Not Allowed", request, clientSocket);
				clientSocket.close();
			}
	        log("Connection with client completed");
	        in.close();
	        clientSocket.close();
		} catch (Exception e){
			e.printStackTrace();
		}
      });
    }
}
   
public static void defaultRequestProcess(Request request, Socket clientSocket) throws IOException {
	String path = resourceDirectory+File.separator+request.getResourceRequested()+"."+request.getFileExtension();
	File file;
	InputStream input;
	try { 
		file = new File(path);
		input = new FileInputStream(file);
	} catch (Exception e) {
		HashMap<String, String> map = request.getReplacesMap();
		map.put("errorMessage", "Page not found");
		request.setReplacesMap(map);
		displayErrorPage("404", "Page not found", request, clientSocket);
		return;
	}
	write(clientSocket.getOutputStream(),"HTTP/1.0 200 OK\r\n");
	write(clientSocket.getOutputStream(),"Date: "+new Date()+"\r\n");
	write(clientSocket.getOutputStream(),"Server: CamsHTTPServer/1.0.0\r\n");
	write(clientSocket.getOutputStream(),"Content-Type: "+contentTypeMap.getOrDefault(request.getFileExtension(), "text/html")+"\r\n");
	write(clientSocket.getOutputStream(),"Content-Length: "+file.length()+"\r\n");
	write(clientSocket.getOutputStream(),"Last-modified: "+new Date()+"\r\n");
	write(clientSocket.getOutputStream(),"\r\n");
	byte[] buf = new byte[500000000];
	int n;
	while ((n = input.read(buf)) != -1) 
		 clientSocket.getOutputStream().write(buf, 0, n); 	
	input.close();
}

public static void displayErrorPage(String statusCode, String errorMessage, Request request, Socket clientSocket) throws IOException{
	String content = new String (Files.readAllBytes( Paths.get(resourceDirectory+File.separator+"errorPage.html")));
	content = replace(content, request.getReplacesMap());
	write(clientSocket.getOutputStream(),"HTTP/1.0 "+statusCode+" OK\r\n");
	write(clientSocket.getOutputStream(),"Date: "+new Date()+"\r\n");
	write(clientSocket.getOutputStream(),"Server: CamsHTTPServer/1.0.0\r\n");
	write(clientSocket.getOutputStream(),"Content-Type: text/html\r\n");
	write(clientSocket.getOutputStream(),"Content-Length: "+content.length()+errorMessage.length()+"\r\n");
	write(clientSocket.getOutputStream(),"Last-modified: "+new Date()+"\r\n");
	write(clientSocket.getOutputStream(),"\r\n");
	write(clientSocket.getOutputStream(),errorMessage+"\r\n");
	write(clientSocket.getOutputStream(),content+"\r\n");
}

public static void customResponse(Socket clientSocket, String response) throws IOException{
	write(clientSocket.getOutputStream(),"HTTP/1.0 200 OK\r\n");
	write(clientSocket.getOutputStream(),"Date: "+new Date()+"\r\n");
	write(clientSocket.getOutputStream(),"Server: CamsHTTPServer/1.0.0\r\n");
	write(clientSocket.getOutputStream(),"Content-Type: text/html\r\n");
	write(clientSocket.getOutputStream(),"Content-Length: "+response.length()+"\r\n");
	write(clientSocket.getOutputStream(),"Last-modified: "+new Date()+"\r\n");
	write(clientSocket.getOutputStream(),"\r\n");
	write(clientSocket.getOutputStream(),response+"\r\n");
}
   
public static void write(OutputStream output, String s) throws IOException {
	    output.write(s.getBytes());
}

public static String compileAndRun(String fileName, Request request, Socket clientSocket)  throws IOException{
	try {
		if (!new File(generatedDirectory+File.separator+fileName+".class").exists())
			compileRuntime(fileName);
		return runClass(fileName, request, clientSocket);
	} catch (Exception e) {
		e.printStackTrace();
		return "ERROR";
	}
}

public static void compileRuntime(String fileName) throws IOException{
	  log("Attempting compilation on "+ resourceDirectory+File.separator+fileName+".java");
	  if (ToolProvider.getSystemJavaCompiler().run(null, null, null, resourceDirectory+File.separator+fileName+".java")==0)
	  	log("Compilation successful");
	  else
		log("Compilation failed");
	  Files.move(Paths.get(resourceDirectory+File.separator+fileName+".class"),
			     Paths.get(generatedDirectory+File.separator+fileName+".class"));
	}

public static String runClass(String fileName, Request request, Socket clientSocket){
	 try {
	    Class<?> c = Class.forName(fileName);
	    Method main = null;
	    if (request.getMethod().equalsIgnoreCase("Get")) 
		    main = c.getDeclaredMethod("doGet", new Class[] { Request.class, Socket.class });
	    if (request.getMethod().equalsIgnoreCase("Post")) 
	    	main = c.getDeclaredMethod("doPost", new Class[] { Request.class, Socket.class });
	    Object result = main.invoke(c.newInstance(), request, clientSocket);
	    return (String) result;
	 } catch (Exception x) {
	    x.printStackTrace();
	    return "ERROR";
	 }
   }

 public static String replace(String original, HashMap<String, String> replaces){
       Pattern pattern = Pattern.compile("\\$\\{(.*?)}");
       Matcher matcher = pattern.matcher(original);
       StringBuilder builder = new StringBuilder();
       int i = 0;
       while (matcher.find()) {
            String replacement =  replaces.getOrDefault(matcher.group(1), "");
            builder.append(original.substring(i, matcher.start()));
            if (replacement == null) {
                builder.append("");
            } else {
                builder.append(replacement);
                i = matcher.end();
            }
        }
        return builder.toString();
     }

public static void log(String message) throws IOException{
    Writer logger = new BufferedWriter(new FileWriter(
    		new File(System.getProperty("user.dir")).getParent()+File.separator+"log.txt", true));
    logger.append(message+"\n");
    logger.close();
    System.out.println(message);
	}
}

class Request{
	String method;
	String resourceRequested;
	int contentLength;
	String content;
	String query;
	String fileExtension;
	HashMap<String, String> replacesMap = new HashMap<String, String>();
	
	public HashMap<String, String> getReplacesMap(){
		return replacesMap;
	}
	
	public void setReplacesMap(HashMap<String, String> replacesMap){
		this.replacesMap = replacesMap;
	}
	
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getResourceRequested() {
		return resourceRequested;
	}
	public void setResourceRequested(String resourceRequested) {
		this.resourceRequested = resourceRequested;
	}
	public int getContentLength() {
		return contentLength;
	}
	public void setContentLength(int contentLength) {
		this.contentLength = contentLength;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
	}
	public String getFileExtension() {
		return fileExtension;
	}
	public void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension;
	}
}