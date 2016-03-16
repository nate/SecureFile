package com.example.kevi.securefile;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import se.simbio.encryption.Encryption;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ChatFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private EditText mInputMessageView;
    private RecyclerView mMessagesView;
    private OnFragmentInteractionListener mListener;
    private List<Message> mMessages = new ArrayList<Message>();
    private RecyclerView.Adapter mAdapter;
    private BigInteger S;
    private String name;
    public BigInteger Primeq;
    public BigInteger Primea;
    Random rnd = new Random();
    private BigInteger XB = new BigInteger(10, rnd);
    BigInteger YA;
    BigInteger YB;

    private Socket socket;
    {
        try{
            socket = IO.socket("http://10.200.113.111:3000");
        }catch(URISyntaxException e){
            throw new RuntimeException(e);
        }
    }
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ChatFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChatFragment newInstance(String param1, String param2) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

            socket.connect();
            join();
            socket.on("message", handleIncomingMessages);
            //dh from server
            socket.on("primeq", handleIncomingPrimeq);
            socket.on("primea", handleIncomingPrimea);
            socket.on("YA", handleIncomingYA);

    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mAdapter = new MessageAdapter( mMessages);
        /*try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }*/

    }
    public void join(){
        try {
            InputStream inputStream = new FileInputStream("UserConfig.txt");

            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString = "";
            StringBuilder stringBuilder = new StringBuilder();

            while ( (receiveString = bufferedReader.readLine()) != null ) {
                stringBuilder.append(receiveString);
            }

            inputStream.close();
            String ret = stringBuilder.toString();
            String[] seperated = ret.split(":");
            name = seperated[0];
            S = new BigInteger(seperated[1]);

            socket.emit("join", name);


        } catch (FileNotFoundException e) {
            name="keeev";
            socket.emit("join",name);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMessagesView = (RecyclerView) view.findViewById(R.id.messages);
        mMessagesView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mMessagesView.setAdapter(mAdapter);

        ImageButton sendButton = (ImageButton) view.findViewById(R.id.send_button);
        mInputMessageView = (EditText) view.findViewById(R.id.message_input);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    sendMessage();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
        });


    }
    private void sendMessage() throws NoSuchAlgorithmException {

        Encryption encryption= new Encryption.Builder()
                .setKeyLength(128)
                .setKeyAlgorithm("AES")
                .setCharsetName("UTF8")
                .setIterationCount(65536)
                .setKey(S.toString())
                .setDigestAlgorithm("SHA1")
                .setSalt(name)
                .setBase64Mode(Base64.DEFAULT)
                .setAlgorithm("AES/CBC/PKCS5Padding")
                .setSecureRandomAlgorithm("SHA1PRNG")
                .setSecretKeyType("PBKDF2WithHmacSHA1")
                .setIv(new byte[]{29, 88, -79, -101, -108, -38, -126, 90, 52, 101, -35, 114, 12, -48, -66, -30})
                .build();

        String message = mInputMessageView.getText().toString().trim();
        mInputMessageView.setText("");
        String encrypted = encryption.encryptOrNull(message);
        addMessage(message);
        JSONObject sendText = new JSONObject();
        try{
            sendText.put("text", encrypted);
            socket.emit("message", sendText);
        }catch(JSONException e){

        }

    }

    public void sendImage(String path)
    {
        JSONObject sendData = new JSONObject();
        try{
            sendData.put("image", encodeImage(path));
            Bitmap bmp = decodeImage(sendData.getString("image"));
            addImage(bmp);
            socket.emit("message",sendData);
        }catch(JSONException e){

        }
    }

    private void addMessage(String message) {

        mMessages.add(new Message.Builder(Message.TYPE_MESSAGE)
                .message(message).build());
        // mAdapter = new MessageAdapter(mMessages);
        mAdapter = new MessageAdapter( mMessages);
        mAdapter.notifyItemInserted(0);
        scrollToBottom();
    }

    private void addImage(Bitmap bmp){
        mMessages.add(new Message.Builder(Message.TYPE_MESSAGE)
                .image(bmp).build());
        mAdapter = new MessageAdapter( mMessages);
        mAdapter.notifyItemInserted(0);
        scrollToBottom();
    }
    private void scrollToBottom() {
        mMessagesView.scrollToPosition(mAdapter.getItemCount() - 1);
    }

    private String encodeImage(String path)
    {
        File imagefile = new File(path);
        FileInputStream fis = null;
        try{
            fis = new FileInputStream(imagefile);
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }
        Bitmap bm = BitmapFactory.decodeStream(fis);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG,100,baos);
        byte[] b = baos.toByteArray();
        String encImage = Base64.encodeToString(b, Base64.DEFAULT);
        //Base64.de
        return encImage;

    }

    private Bitmap decodeImage(String data)
    {
        byte[] b = Base64.decode(data,Base64.DEFAULT);
        Bitmap bmp = BitmapFactory.decodeByteArray(b,0,b.length);
        return bmp;
    }
    private Emitter.Listener handleIncomingMessages = new Emitter.Listener(){
        @Override
        public void call(final Object... args){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String message;
                    String imageText;
                    try {
                        message = data.getString("text");
                        addMessage(message);

                    } catch (JSONException e) {
                        // return;
                    }
                    try {
                        imageText = data.getString("image");
                        addImage(decodeImage(imageText));
                    } catch (JSONException e) {
                        //retur
                    }

                }
            });
        }
    };
    private Emitter.Listener handleIncomingEncryptedMessages = new Emitter.Listener(){
        @Override
        public void call(final Object... args){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String message;
                    String imageText;
                    try {
                        Encryption encryption= new Encryption.Builder()
                                .setKeyLength(128)
                                .setKeyAlgorithm("AES")
                                .setCharsetName("UTF8")
                                .setIterationCount(65536)
                                .setKey(S.toString())
                                .setDigestAlgorithm("SHA1")
                                .setSalt(name)
                                .setBase64Mode(Base64.DEFAULT)
                                .setAlgorithm("AES/CBC/PKCS5Padding")
                                .setSecureRandomAlgorithm("SHA1PRNG")
                                .setSecretKeyType("PBKDF2WithHmacSHA1")
                                .setIv(new byte[]{29, 88, -79, -101, -108, -38, -126, 90, 52, 101, -35, 114, 12, -48, -66, -30})
                                .build();

                        try {
                            message = data.getString("text");
                            String decrypted = encryption.decryptOrNull(message);
                            addMessage(decrypted);

                        } catch (JSONException e) {
                            // return;
                        }
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }

                    try {
                        imageText = data.getString("image");
                        addImage(decodeImage(imageText));
                    } catch (JSONException e) {
                        //retur
                    }

                }
            });
        }
    };
    private Emitter.Listener handleIncomingYA = new Emitter.Listener(){
        @Override
        public void call(final Object... args){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        String sdata = data.getString("value");
                        YA = new BigInteger(sdata);
                        String s = "YA: " + sdata;
                        addMessage(s);
                        YB = Primea.modPow(XB, Primeq);
                        String ybs = YB.toString();
                        ybs = "YB: " + ybs;
                        addMessage(ybs);
                        String xbs = XB.toString();
                        xbs = "XB: " + xbs;
                        addMessage(xbs);

                        socket.emit("YB", YB);
                        S = YA.modPow(XB, Primeq);
                        String Ss = S.toString();
                        Ss = "S: " + Ss;
                        addMessage(Ss);
                        try {
                            File root = new File(Environment.getExternalStorageDirectory(), "Notes");
                            if (!root.exists()) {
                                root.mkdirs(); // this will create folder.
                            }
                            File filepath = new File(root, "UserConfig.txt");  // file path to save
                            FileWriter writer = new FileWriter(filepath);
                            writer.append(name+ " : ");
                            writer.append(S.toString());

                            writer.flush();
                            writer.close();
                            String m = "File generated with name " + "UserConfig.txt";
                            addMessage(m);
                        } catch (Exception e) {

                            e.printStackTrace();

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
        }
    };
    private Emitter.Listener handleIncomingPrimea = new Emitter.Listener(){
        @Override
        public void call(final Object... args){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        String sdata = data.getString("value");
                        Primea=new BigInteger(sdata);
                        String s ="Prime A: "+sdata;
                        addMessage(s);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };
    private Emitter.Listener handleIncomingPrimeq = new Emitter.Listener(){
        @Override
        public void call(final Object... args){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        String sdata = data.getString("value");
                        Primeq=new BigInteger(sdata);
                        String s ="Prime Q: "+sdata;
                        addMessage(s);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
        }
    };
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        socket.disconnect();
    }

}
