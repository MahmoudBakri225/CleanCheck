package demo;

import java.io.File;
import java.io.ObjectInputStream;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;
import javax.crypto.Cipher;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * INTENTIONALLY INSECURE. Open this folder in CleanCheck to see the Security Review tab react.
 * Never copy this code into a real project. It is not compiled or used by the build.
 */
public class VulnerableDemo {

    private static final String DB_PASSWORD = "S3cret!2024";

    public ResultSet findUser(Connection con, HttpServletRequest request) throws Exception {
        String name = request.getParameter("name");
        Statement statement = con.createStatement();
        return statement.executeQuery("SELECT * FROM users WHERE name = '" + name + "'");
    }

    public Connection connect() throws Exception {
        return DriverManager.getConnection("jdbc:mysql://localhost/app", "root", "toor");
    }

    public byte[] hash(byte[] data) throws Exception {
        return MessageDigest.getInstance("MD5").digest(data);
    }

    public Cipher cipher() throws Exception {
        return Cipher.getInstance("AES");
    }

    public String newToken() {
        return Long.toHexString(new Random().nextLong());
    }

    public void ping(String host) throws Exception {
        Runtime.getRuntime().exec("ping -c 1 " + host);
    }

    public File open(String userFile) {
        return new File("uploads/" + userFile);
    }

    public Object load(java.io.InputStream in) throws Exception {
        return new ObjectInputStream(in).readObject();
    }

    public void trustEveryone() {
        HttpsURLConnection.setDefaultHostnameVerifier((host, session) -> true);
    }

    public Object parse() throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    public void read() {
        try {
            new java.io.FileReader("data.txt").close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
