# CamsServer
A Java based HTTP/Application server focused on simplifying the web development process.


This server is an exteremly lightweight http java server based on simplicity.
The goal is to simplify page -> API development.


*******USAGE**********
1. By default, this operates on port 80. To change this, in your  
run script, simple add the port number as an argument on the Java line.

IE- java PageServer would become java PageServer 8080 to run on port 8080.

By default, all activity is logged to the log found in this folder. This 
will be customizable in future updates.

This is currently only working and testing for Windows- I found some strange bugs 
when testing on a Linux machine and have not had time to investigate.

*****Creating pages and server logic*****
Examples of pages and server logic can be found in the resources folder.

While these are example files that can be deleted or replaced, the index 
page and error page are special pages that must remain (can be changed though!).

Do not delete the Request.class or PageServer.class found in the generated folder.

The loading process of resources is as follows:
	1. When a resource is requested, it will look for a Java file to process 
	   custom logic.
	2. If no Java file is found, it will look for a html page or other resource to load.
	3. If neither of these is found, the error page will display with a file not found.

To use provided functions for the Java logic will load/perform custom logic with 
the associated html page. They must have the same name, with the exception of keeping the 
first letter of the Java class name uppercase to be consistant with naming standards.

If you would like to use the built in error handling feature: Your custom functions can return
a String that either says "SUCCESS" or returns the error message. This will be displayed on the 
error page.


To further explain use, lets look at the two provided examples- Index.java and SecondPage.java
Any time we use this functionality, it must have two methods with the accept names and that
take the exact parameters- 
	doPost(Request request, Socket clientSocket) throws IOException;
	doGet(Request request, Socket clientSocket) throws IOException;
These can return a string or return nothing. 
In the Index.java example, the get call will use the defaultRequestProcess which
does the default action- responding with the html page found at request.getContent()
In the post call, it is just logging the parameters recieved.
There is no error handling used here.


In the secondPage.java, the get call is still doing the default page load, but we are using
error handling here- if an exception occurs, it will return the error message which will then
load on the error.html page. The post action here is using a special method that will not 
respond with a document, but will instead respond with the passed in message- this would 
be useful for a API respond/curl/ajax.

You can also use the PageServer.replace(String original, HashMap<String, String>) to replace 
the regex ${<YOUR-PLACEHOLDER} for dynamic page building.


Questions and inquries can be sent to camerongrande95@gmail.com.
		
