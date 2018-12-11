package com.easy.javacv;

import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Map;

/**
 * 对于facebook、twitter 上传视频api中，大视频都是需要分片上传，一次上传只能几Mb内， 并且是以 multipartfile/form-data 方式分片上传文件
 * 现有两个处理方案
 * 1.将视频文件切割成很多小文件，每个文件保持在几Mb内，但对于facebook api来说，是不可行的，facebook每次上传一个分片后，会将需要上传的startOffset和endOffset返回回来，然后再上传指定位置和长度的二进制文件，这样就需要每收到一个上传分片请求后就需要分割文件，效率较低。推荐第2种。
 * 2.打开一个文件流，每次将指定byte 写入到post 请求的输出流中，通过文件流来控制需要上传的位置和字节数，这样只需要保证这个流存在就可以。
 * 代码如下
 * public String facebookChunkedUpload(String pageOrGroupId, String filePath, String accessToken) {
 *         String baseUrl = "https://graph.facebook.com/" + pageOrGroupId + "/videos";
 *         Long fileSize = new File(filePath).length();
 *         Map<String, String> bodyParams = new HashMap<>();
 *         bodyParams.put("access_token", accessToken);
 *         bodyParams.put("upload_phase", "start");
 *         bodyParams.put("file_size", fileSize.toString());
 *         String initRes = HttpUtil.post(baseUrl, bodyParams);
 *         JSONObject initResJson = JSONObject.parseObject(initRes);
 *         String videoId = initResJson.getString("video_id");
 *         String uploadSessionId = initResJson.getString("upload_session_id");
 *         long startOffset = initResJson.getLong("start_offset");
 *         long endOffset = initResJson.getLong("end_offset");
 *
 *         BufferedInputStream bufferedInputStream = null;
 *
 *         try {
 *             bufferedInputStream = new BufferedInputStream(new FileInputStream(filePath));
 *         } catch (FileNotFoundException e) {
 *             e.printStackTrace();
 *         } finally {
 *         }
 *
 *         int chunkIndex = 0;
 *         while (startOffset < endOffset) {
 *             //上传完毕后，请求返回的startOffset = endOffset
 *             String multipartFileName = "chunk" + chunkIndex+".mp4";
 *             logger.info("hinson'log: startOffset: " + startOffset);
 *             logger.info("hinson'log: endOffset: " + endOffset);
 *             String uploadChunkRes = facebookChunkedUploadChunk(pageOrGroupId, accessToken, uploadSessionId,
 *                     multipartFileName, bufferedInputStream, startOffset, endOffset);
 *             logger.info("hinson'log: uploadChunkRes: " + uploadChunkRes);
 *             JSONObject uploadChunkResJson = JSONObject.parseObject(uploadChunkRes);
 *             JSONObject data = uploadChunkResJson.getJSONObject("data");
 *             startOffset = data.getLong("start_offset");
 *             endOffset = data.getLong("end_offset");
 *         }
 *
 *         //上传完毕，
 *         String finishRes = facebookChunkedUploadVideoFinish(pageOrGroupId, uploadSessionId, accessToken);
 *         JSONObject finishResJson = JSONObject.parseObject(finishRes);
 *         if (finishResJson.getBoolean("success") == true) {
 *             return "上传成功";
 *         }else {
 *             return "上传失败";
 *         }
 *     }
 * 作者：苏小小北
 * 链接：https://www.jianshu.com/p/25ae8a91732b
 * 來源：简书
 * 简书著作权归作者所有，任何形式的转载都请联系作者获得授权并注明出处。
 */
public class MultipartfileSplitData {

    /**
     * 以 multipartfile/form-data 方式分片上传文件
     * @param url post请求的url
     * @param params 需要的body参数
     * @param header 需要的header参数
     * @param bufferedInputStream 文件缓存流
     * @param startOffset 上传的起始位置（包含这个位置）
     * @param endOffset 上传的中止位置（包含这个位置）
     * @param multipartFileParam multipartFile中的name
     * @param multipartFileName multipartFile中的fileName
     * @param charset 字符集
     * @param connectTimeout 超时时长，单位毫秒
     * @param readTimeout 超时时长，单位毫秒
     * @return
     */
    public static String postChunkInputStream(String url, Map<String, String> params, Map<String, String> header,
                                              BufferedInputStream bufferedInputStream, Long startOffset, Long endOffset,
                                              String multipartFileParam, String multipartFileName,

                                              String charset, int connectTimeout, int readTimeout) {
        byte[]  BUFFER=new byte[1024*2];
//        try {
//            /**
//             * 文件输入跳到指定的开始位置
//             */
//            bufferedInputStream.skip(startOffset);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        String result = "";
        JSONObject resJson = new JSONObject();
        OutputStream out = null;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);

            if (header != null) {
                for (Map.Entry<String, String> entry : header.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            String boundary = "=====" + String.valueOf(new Date().getTime()) + "=====";
            connection.setRequestProperty("content-type", "multipart/form-data; boundary=" + boundary);
            out = new DataOutputStream(connection.getOutputStream());
            if (params != null && params.size() > 0) {
                StringBuilder stringBuilder = new StringBuilder();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    stringBuilder.append("--" + boundary + "\r\n");
                    stringBuilder.append("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n");
                    stringBuilder.append(entry.getValue() + "\r\n");
                }
                out.write(stringBuilder.toString().getBytes(charset));
            }

            out.write(("--" + boundary + "\r\n").getBytes(charset));
            out.write(("Content-Disposition: form-data; name=\"" + multipartFileParam + "\"; filename=\"" + multipartFileName + "\"\r\n").getBytes(charset));
            out.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes(charset));

            long posOffset = startOffset;
            int readCount;
            while (endOffset - posOffset > BUFFER.length) {
                //剩余要读取的字节数要大于等于BUFFER的长度, write整个BUFFER
                readCount = bufferedInputStream.read(BUFFER);
                out.write(BUFFER, 0, readCount);
                posOffset += readCount;
            }
            if (endOffset - posOffset - 1 > 0) {
                int leftSize = (int)(endOffset-posOffset+1);
                byte[] tmpBuffer = new byte[leftSize];
                //读取剩下的不足BUFFER.length
                readCount = bufferedInputStream.read(tmpBuffer);
                out.write(tmpBuffer, 0, readCount);
                posOffset += readCount;
            }
            out.write("\r\n".getBytes(charset));

            out.write(("--" + boundary + "--\r\n").getBytes(charset));

            out.flush();
            out.close();

            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            if (connection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
                InputStream is = connection.getInputStream();
                while ((readCount = is.read(BUFFER)) > 0) {
                    bout.write(BUFFER, 0, readCount);
                }
                is.close();
            }else {
                InputStream is = connection.getErrorStream();
                while ((readCount = is.read(BUFFER)) > 0) {
                    bout.write(BUFFER, 0, readCount);
                }
                is.close();
            }
            connection.disconnect();
            result = bout.toString();
            resJson.put("code", connection.getResponseCode());
            resJson.put("data", result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resJson.toString();
    }

}
