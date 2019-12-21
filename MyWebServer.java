/*--------------------------------------------------------

1. Name / Date: Adam Brown / May, 9 2019

2. Java version used, if not the official version for the class: 11.0.1

3. Precise command-line compilation examples / instructions:

Windows
> javac MyWebServer.java 
or
> javac *.java

Linux/macOS
$ javac MyWebServer.java
or 
$ javac *.java

4. Precise examples / instructions to run this program:

Windows
> java MyWebServer

Linux/MacOS
$ java MyWebServer

5. List of files needed for running the program.

MyWebServer.java

5. Notes:

This program was written on macOS and uses Linux/macOS naming conventions
for paths of files and directories. The program has not been tested on a 
Windows machine. Please see serverlog.txt for console output of file return 
and directory traversal if this causes an issue. 

Regarding subdirectories, a null pointer exception may be shown on the 
server console if a hot-link is not clicked within the first few seconds of
a suddirectory's contents being displayed. This will not interrupt the program
and selections can still be made.
----------------------------------------------------------*/

import java.io.*;
import java.net.*;
import java.util.ArrayList;

class WebWorker extends Thread {
    Socket sock;

    WebWorker(Socket s) {this.sock = s;}

    @Override
    public void run() {
        PrintStream out = null;
        BufferedReader in = null;

        try {
            //Used to send information to the browser
            out = new PrintStream(sock.getOutputStream());
            //Used to receive information from the browser
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            //For serverlog.txt
            System.out.println("Connection made");

            //Used to hold first line of GET message from browser
            String fromBrowser;
            fromBrowser = in.readLine();

            //For serverlog.txt
            System.out.print("Request from browser: ");
            System.out.println(fromBrowser);

            //Separate the elements in the first line of GET message from browser
            String[] GETInfo = fromBrowser.split(" ");

            /*
            Isolate the file or directory name being requested. Determine
            whether it is the root directory.
            */
            String fileOrDirectoryName;
            if(GETInfo[1].length() == 1)
                fileOrDirectoryName = GETInfo[1];
            else
                fileOrDirectoryName = GETInfo[1].substring(1);

            /*
            First decide whether the addnums method should be called.
            If not, determine whether a file or directory was requested
            by the browser and call the corresponding method.
             */
            if(fileOrDirectoryName.contains("cgi?person"))
                addnums(out, fileOrDirectoryName);
            else {
                if (fileOrDirectoryName.endsWith("/"))
                    sendDirectory(out, fileOrDirectoryName);
                else
                    sendFile(out, fileOrDirectoryName);
            }

        } catch(IOException x) {
            System.out.println("Server/Client connection error");
        }
    }

    static void sendFile(PrintStream out, String fileName) throws IOException {
        File fileToSend = null;
        String fileExtension;
        long contentLength;
        String contentType;
        //Will be used to hold the content of the file and assist in sending it to the browser
        ArrayList<String> fileContent = new ArrayList<String>();

        //For serverlog.txt
        System.out.println("Sending file: " + fileName);
        System.out.println();
        
        /*
        Determine whether this file lives in the root directory or a subdirectory.
        Create a java File object for the file for later use.
         */
        if(fileName.contains("/"))
            fileToSend = new File("./" + fileName);
        else
            fileToSend = new File(fileName);

        //Send a console message if the file cannot be found
        if(!fileToSend.exists())
            System.out.println("No such file exists in this directory");

        //Isolate the file extension
        fileExtension = fileName.split("\\.")[1];

        //Store the size of this file in bytes
        contentLength = fileToSend.length();

        //Determine the MIME type for this file based on its extension
        switch(fileExtension) {
            case "txt" :
                contentType = "text/plain";
                break;
            case "java" :
                contentType = "text/plain";
                break;
            case "html" :
                contentType = "text/html; charset=iso-8859-1";
                break;
            default :
                contentType = "text/plain";
        }

        /*
        Create a BufferedReader and use it to read the file. Store the contents of
        the file in an ArrayList that will later be used for output to the browser.
         */
        BufferedReader fileBR = new BufferedReader(new FileReader(fileToSend));
        boolean endOfFileReached = false;
        while (endOfFileReached == false) {
            String lineFromFile = fileBR.readLine();
            if (lineFromFile != null)
                fileContent.add(lineFromFile);
            else
                endOfFileReached = true;
        }

        //Send http response headers to the browser using the information collected above
        out.print("HTTP/1.1 200 OK\r\n");
        out.print("Accept-Ranges: bytes\r\n");
        out.print("Content-Length: " + contentLength + "\r\n");
        out.print("Content-Type: " + contentType + "\r\n");
        out.print("Connection: close\r\n");
        out.print("\r\n");
        //Send the content of the file
        for(int i=0; i<fileContent.size(); i++)
            out.print(fileContent.get(i) + "\r\n");
    }

    static void sendDirectory(PrintStream out, String directoryName) throws IOException {
        String root = "./";
        String directoryToDisplayName;
        File directoryToDisplay = null;
        //Will accumulate the bytes needed to be sent to the browser
        int byteCounter;
        //Will hold the total number of bytes to be sent to the browser
        long contentLength;
        String[] directoryContents = null;

        //Establish the directory path, remove the trailing "/"
        directoryToDisplayName = root + directoryName.substring(0, directoryName.length()-1);

        /*
        Create a java File object for the directory. Send a server console
        message if the directory cannot be found.
         */
        directoryToDisplay = new File(directoryToDisplayName);
        if(!directoryToDisplay.exists())
            System.out.println("No such directory exists in this location");

        /*
        Formatting for the first and second lines of html to be sent to the browser.
        Include the directory path in the second line which will be displayed by the
        browser.
         */
        String fl = "<pre>\r\n";
        String sl = null;
        if(directoryToDisplayName.endsWith("/"))
            sl = "<h1>" + "Index of " + directoryToDisplayName + "</h1>\r\n";
        else
            sl = "<h1>" + "Index of " + directoryToDisplayName + "/" + "<h1>\r\n";

        //Store the bytes of the first two lines of html
        byteCounter = fl.length() + sl.length() + 1;

        /*
        Create a prefix and a suffix to add to the strings that represent the
        files and subdirectories of this directory. Necessary for sending them
        to the browser as html.
         */
        String linePre = "<a href=\"" + root;
        String lineSuf = "</a> <br>\r\n";

        //Will be used to store the files and subdirectories of this directory as html
        ArrayList<String> bodyLines = new ArrayList<String>();
        String bodyLine = null;

        /*
        Identify the files and subdirectories of this directory and store their names
        as Strings in an array. Iterate through that array. Format the strings so
        that they can be sent as html and add them to the ArrayList bodyLines. Add the
        bytes of the formatted strings to byteCounter.
         */
        directoryContents = directoryToDisplay.list();
        if(directoryContents != null) {
            for(int i=0; i<directoryContents.length; i++) {
                String fd = directoryContents[i];
                File f = null;

                if(directoryToDisplayName == root)
                    f = new File(fd);
                else
                    f = new File(directoryToDisplayName + "/" + fd);

                if(f.isDirectory())
                    bodyLine = linePre + fd + "/\">" + fd + "/" + lineSuf;
                else
                    bodyLine = linePre + fd + "\">" + fd + lineSuf;
                
                byteCounter += bodyLine.length();
                bodyLines.add(bodyLine);
            }
        }

        //Finalize the number of bytes to be sent to the browser
        contentLength = byteCounter;

        //For serverlog.txt
        if(directoryName.equals("/"))
            System.out.println("Sending index of ./");
        else
            System.out.println("Sending index of " + directoryToDisplayName + "/");
        for(int i=0; i<directoryContents.length; i++)
            System.out.println(directoryContents[i]);
        System.out.println();

        //Send http headers to the browser using the information collected above
        out.print("HTTP/1.1 200 OK\r\n");
        out.print("Accept-Ranges: bytes\r\n");
        out.print("Content-Length: " + contentLength + "\r\n");
        out.print("Content-Type: text/html; charset=iso-8859-1\r\n");
        out.print("Connection: close\r\n");
        out.print("\r\n");
        out.print(fl);
        out.print(sl);
        //Send the content of the directory
        if(directoryContents != null) {
            for (int i = 0; i < bodyLines.size(); i++)
                out.print(bodyLines.get(i));
        }
    }

    static void addnums(PrintStream out, String formInfo) {
        String name;
        int num1;
        int num2;
        int sum;
        int byteCounter;
        long contentLength;

        /*
        Separate the message received from the browser into elements which can
        then be used to isolate name, num1, and num2.
         */
        String[] elements = formInfo.split("=");
        name = elements[1].substring(0, elements[1].indexOf("&"));
        String num1string = elements[2].substring(0, elements[2].indexOf("&"));
        String num2string = elements[3];
        num1 = Integer.parseInt(num1string);
        num2 = Integer.parseInt(num2string);

        //Compute the sum of the two numbers entered by the user on the client side
        sum = num1 + num2;

        //Will be sent as part of the outgoing html message
        String fl = "<pre>\r\n";
        byteCounter = fl.length() + 1;

        //A prefix and suffix used for formatting the outgoing html message
        String pre = "<h1>Hi ";
        String suf = "</h1>\r\n";
        //Arrange/format the message that will be displayed in the browser
        String sl = pre + name + ", " + num1 + " + " + num2 + " = " + sum + "." + suf;

        //Finalize the number of bytes being sent, assign to contentLength
        byteCounter += sl.length();
        contentLength = byteCounter;

        //For serverlog,.txt
        System.out.println("Sending sum to browser");
        System.out.println();

        //Send http headers and message to the browser
        out.print("HTTP/1.1 200 OK\r\n");
        out.print("Accept-Ranges: bytes\r\n");
        out.print("Content-Length: " + contentLength + "\r\n");
        out.print("Content-Type: text/html; charset=iso-8859-1\r\n");
        out.print("Connection: close\r\n");
        out.print("\r\n");
        out.print(fl);
        out.print(sl);
    }
}

//This class taken from MyListener.java
public class MyWebServer {
    public static boolean controlSwitch = true;

    public static void main(String a[]) throws IOException {
        int q_len = 6;
        int port = 2540;
        Socket sock;

        ServerSocket servSock = new ServerSocket(port, q_len);

        System.out.println("Adam Brown's web server running at 2540. \n");

        while(controlSwitch) {
            //Create socket, wait for browser connection
            sock = servSock.accept();
            //Spawn WebWorker thread to handle browser requests
            new WebWorker(sock).start();
        }
    }
}