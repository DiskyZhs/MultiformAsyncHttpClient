package cn.com.flyerdream.multiformhttpclientlibrary;

import com.loopj.android.http.RequestParams;
import com.loopj.android.http.ResponseHandlerInterface;

import org.apache.http.HttpEntity;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 扩充与开源项目AsyncHttpClient，用于一次性传输对各InputStream
 * Created by ZhangHaoSong on 2016/04/21.
 */
public class CustomRequestParams extends RequestParams {
    protected final ConcurrentHashMap<String, List<StreamWrapper>> streamArrayParams = new ConcurrentHashMap<String, List<StreamWrapper>>();


    /**
     * Adds files array to the request with both custom provided file content-type and files names
     *
     * @param key             the key name for the new param.
     * @param files           the files array to add.
     * @param contentType     the content type of the file, eg. application/json
     * @param customFileNames file name array to use instead of real file name
     * @throws FileNotFoundException throws if wrong File argument was passed
     */
    public void put(String key, File files[], String contentType, String[] customFileNames) throws FileNotFoundException {
        if (key != null) {
            List<FileWrapper> fileWrappers = new ArrayList<FileWrapper>();
            for (int i = 0; i < files.length; i++) {
                if (files[i] == null || !files[i].exists()) {
                    throw new FileNotFoundException();
                }
                fileWrappers.add(new FileWrapper(files[i], contentType, customFileNames[i]));
            }
            fileArrayParams.put(key, fileWrappers);
        }
    }

    /**
     * Adds InputStream array to the request with both custom content-type and names
     *
     * @param key
     * @param inputStreams
     * @param contentType
     * @param customFileNames
     */
    public void put(String key, InputStream[] inputStreams, String contentType, String[] customFileNames) {
        if (key != null) {
            List<StreamWrapper> streamWrappers = new ArrayList<StreamWrapper>();
            for (int i = 0; i < inputStreams.length; i++) {
                streamWrappers.add(new StreamWrapper(inputStreams[i], contentType, customFileNames[i], true));
            }
            streamArrayParams.put(key, streamWrappers);
        }
    }

    /**
     * Adds InputStream array to the request with both custom content-type and names
     */
    public void put(String key, ArrayList<InputStream> inputStreams, String contentType, ArrayList<String> customFileNames) {
        if (key != null) {
            List<StreamWrapper> streamWrappers = new ArrayList<StreamWrapper>();
            for (int i = 0; i < inputStreams.size(); i++) {
                streamWrappers.add(new StreamWrapper(inputStreams.get(i), customFileNames.get(i), contentType, true));
            }
            streamArrayParams.put(key, streamWrappers);
        }
    }

    @Override
    public boolean has(String key) {
        return super.has(key) || streamArrayParams.get(key) != null;
    }


    @Override
    public String toString() {
        return super.toString() + "CustomRequestParams{" +
                "streamArrayParams=" + streamArrayParams +
                '}';
    }

    @Override
    public void remove(String key) {
        streamArrayParams.remove(key);
        super.remove(key);
    }


    private HttpEntity createMultipartEntity(ResponseHandlerInterface progressHandler) throws IOException {
        SimpleMultipartEntity entity = new SimpleMultipartEntity(progressHandler);
        entity.setIsRepeatable(isRepeatable);

        // Add string params
        for (ConcurrentHashMap.Entry<String, String> entry : urlParams.entrySet()) {
            entity.addPartWithCharset(entry.getKey(), entry.getValue(), contentEncoding);
        }

        // Add non-string params
        List<BasicNameValuePair> params = getParamsList();
        for (BasicNameValuePair kv : params) {
            entity.addPartWithCharset(kv.getName(), kv.getValue(), contentEncoding);
        }

        // Add stream params
        for (ConcurrentHashMap.Entry<String, StreamWrapper> entry : streamParams.entrySet()) {
            StreamWrapper stream = entry.getValue();
            if (stream.inputStream != null) {
                entity.addPart(entry.getKey(), stream.name, stream.inputStream,
                        stream.contentType);
            }
        }

        //Add stream collection(ZHS)
        for (ConcurrentHashMap.Entry<String, List<StreamWrapper>> entry : streamArrayParams.entrySet()) {
            List<StreamWrapper> streamWrapper = entry.getValue();
            for (StreamWrapper sw : streamWrapper) {
                entity.addPart(entry.getKey(), sw.name, sw.inputStream, sw.contentType);
            }
        }

        // Add file params
        for (ConcurrentHashMap.Entry<String, FileWrapper> entry : fileParams.entrySet()) {
            FileWrapper fileWrapper = entry.getValue();
            entity.addPart(entry.getKey(), fileWrapper.file, fileWrapper.contentType, fileWrapper.customFileName);
        }

        // Add file collection
        for (ConcurrentHashMap.Entry<String, List<FileWrapper>> entry : fileArrayParams.entrySet()) {
            List<FileWrapper> fileWrapper = entry.getValue();
            for (FileWrapper fw : fileWrapper) {
                entity.addPart(entry.getKey(), fw.file, fw.contentType, fw.customFileName);
            }
        }

        return entity;
    }

    @Override
    public HttpEntity getEntity(ResponseHandlerInterface progressHandler) throws IOException {
        if (useJsonStreamer) {
            return super.getEntity(progressHandler);
        } else if (!forceMultipartEntity && streamParams.isEmpty() && fileParams.isEmpty() && fileArrayParams.isEmpty() && streamArrayParams.isEmpty()) {
            return super.getEntity(progressHandler);
        } else {

            return createMultipartEntity(progressHandler);
        }
    }
}
