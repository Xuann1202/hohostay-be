package tw.com.ispan.eeit.ho_back.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ReadImage {
    public static byte[] readFromUrl(String urlPath) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlPath)).build();
        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                return response.body();
            }
        } catch (Exception e) {
            System.out.println("網頁讀取照片失敗" + e.getMessage());
        }
        return null;
    }

    public static String getContentType(String filename) {
        String fileExtension = filename.substring(filename.lastIndexOf('.') + 1);
        System.out.println("fileExtension" + fileExtension);
        String contentType = "image/" + fileExtension;
        return contentType;
    }
}
