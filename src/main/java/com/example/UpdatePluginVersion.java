package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;

import com.atlassian.bamboo.build.logger.BuildLogger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.log4j.Logger;

/**
 * Implementation of a {@link Builder} step to refresh an ICN plug-in.
 * 
 * @author Guillaume Delory
 * Date:   Jan 30, 2017
 * 
 *
 */
public class UpdatePluginVersion{

    /**
     * ICN desktop to use for the admin, any desktop can be used but hard coded
     * admin will always work and make configuration easier for users than asking them for one 
     */
    private static final String DESKTOP = "admin";
    private static final String SAVE_URL = "jaxrs/admin/configuration";
    private static final String LOAD_URL = "jaxrs/admin/loadPlugin";
    private static final String LOGON_URL = "jaxrs/logon";
    private String url;
    private String file;
    private String username;
    private String password;
    
    private transient String eUrl;
    private transient String eFile;
    private transient String eUsername;
    private transient String ePassword;
    
    
    final static Logger logger = Logger.getLogger(UpdatePluginVersion.class);
    
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    
    public UpdatePluginVersion(String url, String username, String password, String file) {
        this.url = url;
        this.eUrl = url;
        this.username = username;
        this.eUsername = username;
        this.password = password;
        this.ePassword = password;
        this.file = file;
        this.eFile = file;
    }
    
    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public String getFile() {
        return file;
    }
    
    
    public boolean perform(BuildLogger buildLogger) throws IOException, InterruptedException {

        boolean result = false;
        buildLogger.addBuildLogEntry("*****************  BuildLogger *****************");
        HttpClient httpclient = new HttpClient();
        httpclient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        logger.info("perform");
        
      
        
        // Logon, reload and save configuration
        String security_token = logon(httpclient);
        if (security_token != null) {
            JSONObject loadResult = reload(httpclient, security_token);
            if (loadResult != null) {
                try {
                    result = save(httpclient, loadResult, security_token);
                } catch (Exception e) {
                    logger.error("ERROR: Exception while reloading the plugin: " + e.getMessage());                    
                }
            }
        }
        return result;
    }
    
    private boolean safetyChecks(PrintStream log) {
        if (checkEmpty(eFile, "file", log)) return false;
        if (checkEmpty(eUsername, "udername", log)) return false;
        if (checkEmpty(ePassword, "password", log)) return false;
        if (checkEmpty(eUrl, "url", log)) return false;
        if (!eUrl.endsWith("/")) {
        	eUrl += "/";
        }
        return true;
    }

    private boolean checkEmpty(String s, String name, PrintStream log) {
        if (s == null || s.isEmpty()) {
            logger.info(name + " can't be empty.");
            return true;
        } else {
            return false;
        }
    }
    
    private String readOneLineHttp(PostMethod http) throws IOException {
        BufferedReader in = null;
        String res = null;
        try {
            in = new BufferedReader(new InputStreamReader(http.getResponseBodyAsStream(), Charset.defaultCharset()));
            res = in.readLine();
        } catch (IOException e) {        
        	logger.error(e.getMessage());            
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return res;
    }
    
    /**
     * Log on against ICN, this will store the needed cookies and return the 
     * security token header. Both are needed for a successful authentication.
     * @param httpClient the {@link HttpClient} connection to use, this will have
     *                   to be used for all future calls since it gets the
     *                   authentication cookies.
     * @param log the logger as {@link PrintStream}
     * @return the security token, <code>null</code> if anything went wrong.
     *         Exception is already logged if <code>null</code> is returned.
     */
    private String logon(HttpClient httpClient) {
        logger.info("Connecting to ICN as " + eUsername + "...");
        
        String res = null;
        
        PostMethod httpPost = new PostMethod(eUrl + LOGON_URL);
        httpPost.addParameter(new NameValuePair("userid", eUsername));
        httpPost.addParameter(new NameValuePair("password", ePassword));
        httpPost.addParameter(new NameValuePair("desktop", DESKTOP));
        String json = null;
        try {
            httpClient.executeMethod(httpPost);
            System.out.println(httpPost.getStatusLine());
            json = readOneLineHttp(httpPost);
            if (json == null) {
                logger.info("Empty response from the server while logging in.");
                return null;
            }
            // Unsecure the json if prefix is activated in servlet
            if (json.startsWith("{}&&")) {
                json = json.substring(4);
            }
            JSONObject jsonObj = new JSONObject(json);
            
            if (!jsonObj.has("security_token")) {
                logger.info("ERROR: Exception while logging into ICN. Response was " + json);
            } else {
                res = (String) jsonObj.get("security_token");
                if (res != null && !"".equals(res)) {
                    logger.info("OK");
                } else {
                    logger.info("KO");
                }
            }            
        } catch (Exception e) {            
            logger.error(e.getMessage());
            logger.info("Login response was: " + json);
        } finally {
            httpPost.releaseConnection();
        }
        return res;
    }
    
    
    /**
     * Reload the plugin from the given path.
     * @param httpClient the {@link HttpClient} to use, it needs to have the authentication
     *                   cookies, brought by a call to the logon method.
     * @param log The {@link PrintStream} to print information to
     * @param security_token the security token to use as header. This is returned by the
     *                       logon method.
     * @return the result of the call, will be needed to save the configuration
     */
    private JSONObject reload(HttpClient httpClient, String security_token) {
        logger.info("Reloading plugin " + eFile + "...");
        
        JSONObject res = null;
        
        PostMethod httpPost = new PostMethod(eUrl + LOAD_URL);
        httpPost.addParameter(new NameValuePair("fileName", eFile));
        httpPost.addParameter(new NameValuePair("desktop", DESKTOP));

        httpPost.addRequestHeader("security_token", security_token);
        
        String json = null;
        try {
            httpClient.executeMethod(httpPost);
            if (httpPost.getStatusCode()!= 200) {
                logger.info("KO");
                logger.info(LOAD_URL + " returned " + httpPost.getStatusLine());
            } else {
                json = readOneLineHttp(httpPost);
                if (json == null) {
                    logger.info("Empty response from the server while reloading the plugin.");
                    return null;
                }
                // Unsecure the json if prefix is activated in servlet
                if (json.startsWith("{}&&")) {
                    json = json.substring(4);
                }
                res = new JSONObject(json);
                
                if (!res.has("name") || !res.has("id") || !res.has("version") || !res.has("configClass")) {
                    logger.info("KO");
                    logger.info("Response does not have correct attributes: " + json);
                    logger.info("It should contain the following attributes: name, id, version, configClass");
                    res = null;
                } else {
                    logger.info("OK");
                    logger.info("Plug-in " + res.getString("name") + "(id: " + res.getString("id") + ")" + " successfully reloaded.");
                }                                
            }
        } catch (Exception e) {            
            logger.error(e.getMessage());
            logger.info("LoadPlugin response was: " + json);
        } finally {
            httpPost.releaseConnection();
        }
        return res;
    }
    
    
    /**
     * Save the configuration pre-created by the load plugin call.
     * 
     * @param httpClient the {@link HttpClient} to use, it needs to have the authentication
     *                   cookies, brought by a call to the logon method.
     * @param log the {@link PrintStream} to use to print information
     * @param loadResult the resulting {@link JSONObject} from the save operation containing plugin information
     * @param security_token the security token to use as header. This is returned by the
     *                       logon method.
     * @return <code>true</code> if the save is successful
     * @throws JSONException
     */
    private boolean save(HttpClient httpClient, JSONObject loadResult, String security_token) throws JSONException {
        logger.info("Saving configuration...");
        
        boolean res = false;
        
        PostMethod httpPost = new PostMethod(eUrl + SAVE_URL);
        httpPost.addParameter(new NameValuePair("action", "update"));
        httpPost.addParameter(new NameValuePair("id", loadResult.getString("id")));
        httpPost.addParameter(new NameValuePair("configuration", "PluginConfig"));
        httpPost.addParameter(new NameValuePair("desktop", DESKTOP));
        JSONObject json_post = new JSONObject();
        json_post.put("enabled", true);
        json_post.put("filename", eFile);
        json_post.put("version", loadResult.getString("version"));
        json_post.put("dependencies", new JSONArray());
        json_post.put("name", loadResult.getString("name"));
        json_post.put("id", loadResult.getString("id"));
        json_post.put("configClass", loadResult.getString("configClass"));
        httpPost.addParameter(new NameValuePair("json_post", json_post.toString()));
        
        httpPost.addRequestHeader("security_token", security_token);
        String json = null;
        try {
            httpClient.executeMethod(httpPost);
            if (httpPost.getStatusCode() != 200) {
                logger.info("KO");
                logger.info(SAVE_URL + " returned " + httpPost.getStatusLine());
            } else {
                json = readOneLineHttp(httpPost);
                if (json == null) {
                    logger.info("Empty response from the server while saving the configuration.");
                    return false;
                }
                // Unsecure the json if prefix is activated in servlet
                if (json.startsWith("{}&&")) {
                    json = json.substring(4);
                }
                JSONObject jsonObj = new JSONObject(json);
                logger.info("JSON conversion OK");
                JSONArray messages = jsonObj.getJSONArray("messages");
                logger.info("Returned message is:");
                for (int i = 0; i < messages.length(); i++) {
                    logger.info(messages.getJSONObject(i).getString("text"));
                }
                res = true;
            }
        } catch (Exception e) {            
            logger.error(e.getMessage());
            logger.info("configuration response was: " + json);
        } finally {
            httpPost.releaseConnection();
        }
        return res;
    }
   
}


