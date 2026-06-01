package cn.starhelix.material.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

import cn.starhelix.material.data.CacheData;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;

// 该类不再使用
public class SocketClient {
    private static final String TAG = "SocketClient";

    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 10000;

    public static String communicate(String requestString) throws IOException {
        Socket socket = new Socket();
//        SocketAddress socketAddress = new InetSocketAddress(CacheData.getInstance().getHost(),
//                CacheData.getInstance().getPort());
        SocketAddress socketAddress = new InetSocketAddress("127.0.0.1", 6000);
        socket.connect(socketAddress, CONNECT_TIMEOUT_MS);

        if (!socket.isConnected()) {
            throw new IOException("fail to connect to server");
        }
        Log.i(TAG, "server is connected");
//        Log.i(TAG, CacheData.getInstance().getHost());
//        Log.i(TAG, "" +CacheData.getInstance().getPort());

        socket.setSoTimeout(READ_TIMEOUT_MS);
        OutputStream outputStream = socket.getOutputStream();
        Log.i(TAG, requestString);
        outputStream.write(requestString.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();

        Log.i(TAG, "write over");
        InputStream inputStream = socket.getInputStream();
        InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        String s = br.readLine();

//        char[] buffer = new char[1024];
//        int cnt = br.read(buffer, 0, 1024);
//        Log.d(TAG, new String(buffer, 0, cnt));
//        Log.d(TAG, "" + s);

        inputStream.close();
        outputStream.close();
        socket.close();
        return s;
    }

    public static Observable<String> rxCommunicate(String requestString) {
        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<String> emitter) throws Throwable {
                try {
                    String responseStr = communicate(requestString);
                    emitter.onNext(responseStr);
                    emitter.onComplete();
                } catch (IOException e) {
                    emitter.onError(e);
                }
            }
        });
    }
}
