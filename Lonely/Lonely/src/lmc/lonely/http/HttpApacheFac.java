package lmc.lonely.http;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
public class HttpApacheFac {
	private static String CharSet = HTTP.UTF_8;
    private static HttpClient client = null;
    public static synchronized HttpClient getHttpClient(){
    	if(client==null){
    		HttpParams params = new BasicHttpParams();
    		HttpProtocolParams.setVersion(params,HttpVersion.HTTP_1_1);
    		HttpProtocolParams.setContentCharset(params,CharSet);
    		HttpProtocolParams.setUseExpectContinue(params,true);
    		//HttpProtocolParams.setUserAgent(params,"Mozilla/5.0(Linux;U;Android 2.2.1;en-us;Nexus One Build.FRG83) "
            	//+"AppleWebKit/553.1(KHTML,like Gecko) Version/4.0 Mobile Safari/533.1");
    		ConnManagerParams.setMaxConnectionsPerRoute(params,new ConnPerRouteBean(100));
    		ConnManagerParams.setTimeout(params,10000);
    		HttpConnectionParams.setConnectionTimeout(params,15000);
    		HttpConnectionParams.setSoTimeout(params,15000);
    		SchemeRegistry shm = new SchemeRegistry();
    		shm.register(new Scheme("http",PlainSocketFactory.getSocketFactory(),80));
            shm.register(new Scheme("https",SSLSocketFactory.getSocketFactory(),443));
            client = new DefaultHttpClient(new ThreadSafeClientConnManager(params,shm),params);
    	}
		return client;
    }
    public static void closeHttpClient(){
    	if(client!=null){
    		client.getConnectionManager().shutdown();
    	}
    }
    public static HttpClient getHttps(String sslKeyPath,String host,String username,String password){
    	HttpClient client = null;
		try{
			client = new DefaultHttpClient();
    		SSLSocketFactory socket = null;
    		Scheme shm = null;
    		if(sslKeyPath!=null&&!sslKeyPath.equals("")){
    			File keyFile = new File(sslKeyPath);
    			if(keyFile.exists()&&keyFile.isFile()){
    				KeyStore key = KeyStore.getInstance(KeyStore.getDefaultType());
    				FileInputStream fis = new FileInputStream(keyFile);
    				key.load(fis,"password".toCharArray());
    	    		fis.close();
    	    		socket = new SSLSocketFactory(key);
    	    		shm = new Scheme("https",socket,443);
    			}
    		}
    		if(socket==null){
    			shm = new Scheme("https",SSLSocketFactory.getSocketFactory(),443);
    		}
    		client.getConnectionManager().getSchemeRegistry().register(shm);
    		if(host!=null&&username!=null&&password!=null){
    			((AbstractHttpClient)client).getCredentialsProvider().setCredentials(new AuthScope(host,443),new UsernamePasswordCredentials(username,password));
    		}
		}catch(Exception e){
			e.printStackTrace();
		}
    	return client;
    }
    public static String doPost(String url,HashMap<String,Object>argVals){
    	try{
			if(url==null){
				return null;
			}
			HttpPost post = new HttpPost(url);
			if(argVals!=null&&argVals.size()>0){
				ArrayList<NameValuePair>args = new ArrayList<NameValuePair>();
	        	for(Entry<String,Object>e:argVals.entrySet()){
	        		NameValuePair arg = new BasicNameValuePair(e.getKey(),e.getValue().toString());
	        		args.add(arg);
	    		}
	    		post.setEntity(new UrlEncodedFormEntity(args,CharSet));
			}
			HttpResponse resp = HttpApacheFac.getHttpClient().execute(post);
			if(resp.getStatusLine().getStatusCode()==HttpStatus.SC_OK){
    			HttpEntity res = resp.getEntity();
    			if(res!=null){
    				return EntityUtils.toString(res,CharSet);
    			}
            }else{
            	post.abort();
            }
		}catch(Exception e){
			e.printStackTrace();
			return "请求异常";
		}
    	return null;
    }
    public static String getCookies(String url){
    	StringBuffer res = new StringBuffer();
    	try{
        	CookieStore store = new BasicCookieStore();
        	HttpContext con = new BasicHttpContext();
        	con.setAttribute(ClientContext.COOKIE_STORE,store);
        	HttpGet get = new HttpGet(url);
        	HttpResponse resp = HttpApacheFac.getHttpClient().execute(get,con);
        	res.append("请求返回状态信息:"+resp.getStatusLine()+"\n");
        	HttpEntity entity = resp.getEntity();
        	if(entity!=null){
        		res.append("请求返回信息长度:"+entity.getContentLength()+"\n");
        	}
        	Header[]headers = resp.getAllHeaders();
        	int i = 0;
        	for(i=0;i<headers.length;i++){
        		res.append("Header-"+i+"内容:"+headers[i]+"\n");
        	}
        	res.append("头消息共计"+i+"条\n");
        	List<Cookie>cookies = store.getCookies();
        	for(i=0;i<cookies.size();i++){
        		res.append("Cookie-"+i+"内容:"+cookies.get(i).toString()+"\n");
        	}
        	res.append("Cookie共计"+i+"个");
		}catch(Exception e){
			e.printStackTrace();
			return "请求异常";
		}
    	return res.toString();
    }
}