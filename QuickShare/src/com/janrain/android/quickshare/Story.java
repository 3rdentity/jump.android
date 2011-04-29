package com.janrain.android.quickshare;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Config;
import android.util.Log;
import android.view.Gravity;
import com.janrain.android.engage.session.JRSessionData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * Created by IntelliJ IDEA.
 * User: lillialexis
 * Date: 4/22/11
 * Time: 1:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class Story implements Serializable {
    private static final String TAG = Story.class.getSimpleName();

    private String mTitle;
    private String mDate;
    private String mDescription;
    private String mPlainText;
    private String mLink;
    private ArrayList<String> mImageUrls;
    private transient Bitmap mImage;

    private boolean mDescriptionImagesAlreadyScaled;
    private boolean mCurrentlyDownloading;

    public static Story dummyStory() {
        return new Story();
    }

    public Story(String title, String date, String description,
                 String plainText, String link, ArrayList<String> imageUrls) {
        this.mTitle = title;
        this.mDate = date;
        this.mDescription = description;
        this.mPlainText = plainText;
        this.mLink = link;
        this.mImageUrls = imageUrls;

        mDescriptionImagesAlreadyScaled = false;

        if (mImageUrls != null)
            if (!mImageUrls.isEmpty())
                startDownloadImage(mImageUrls.get(0));
    }

    private Story() {
    }

    public void downloadImage() {
        if (mImageUrls != null)
            if (!mImageUrls.isEmpty())
                startDownloadImage(mImageUrls.get(0));
    }

    private void startDownloadImage(final String imageUrl) {
        if (Config.LOGD)
            Log.d(TAG, "[startDownloadImage] " + imageUrl);

        synchronized (this) {
            if (mCurrentlyDownloading) return;
            else mCurrentlyDownloading = true;
        }

        new AsyncTask<Void, Void, Void>(){
            public Void doInBackground(Void... s) {
                if (Config.LOGD)
                    Log.d(TAG, "[doInBackground] downloading image");
                
                try {
                    URL url = new URL(imageUrl);
                    InputStream is = url.openStream();

//                    BitmapDrawable bd = new BitmapDrawable(BitmapFactory.decodeStream(is));
                    Bitmap bd = BitmapFactory.decodeStream(is);

                    int width = bd.getWidth();// .getIntrinsicWidth();
                    int height = bd.getHeight();// .getIntrinsicHeight();

                    if (Config.LOGD)
                        Log.d(TAG, "[getView] original image size: " +
                                ((Integer)width).toString() + ", " + ((Integer)height).toString());

                    int x_ratio = width/120, y_ratio = height/120, scale_ratio;

                    if (x_ratio <= 1 || y_ratio <= 1)
                        scale_ratio = 1;
                    else if (x_ratio < y_ratio)
                        scale_ratio = x_ratio;
                    else
                        scale_ratio = y_ratio;

                    mImage = Bitmap.createScaledBitmap(bd, width/scale_ratio, height/scale_ratio, true);

                    if (Config.LOGD)
                        Log.d(TAG, "[getView] scaled image size: " +
                                ((Integer)mImage.getWidth()).toString() + ", " +
                                ((Integer)mImage.getHeight()).toString());

                    //ScaleDrawable sd = ScaleDrawable.createFromStream(is, "stream");

//                    int width = bd.getIntrinsicWidth();
//                    int height = bd.getIntrinsicHeight();
//
//                    if (Config.LOGD)
//                        Log.d(TAG, "[doInBackground] image size: " +
//                                ((Integer)width).toString() + ", " + ((Integer)height).toString());
//
//                    if (width > 120 && height > 90)
//                        mImage = new ScaleDrawable(bd, Gravity.CLIP_HORIZONTAL | Gravity.CLIP_VERTICAL, width/3, height/3);
//                    else
//                        mImage = new ScaleDrawable(bd, Gravity.CLIP_HORIZONTAL | Gravity.CLIP_VERTICAL, width, height);

                } catch (MalformedURLException e) {
                    if (Config.LOGD) Log.d(TAG, e.toString());
                } catch (IOException e) {
                    if (Config.LOGD) Log.d(TAG, e.toString());
                } catch (RuntimeException e) {
                    if (Config.LOGD) Log.d(TAG, e.toString());
                } catch (OutOfMemoryError e) {
                    if (Config.LOGD) Log.d(TAG, e.toString());
                }


                mCurrentlyDownloading = false;
                return null;
            }

            protected void onPostExecute(Boolean loadSuccess) {
                Log.d(TAG, "[onPostExecute] image download onPostExecute, result: " +
                        (loadSuccess ? "succeeded" : "failed"));

//                if (loadSuccess)
//                    mListener.AsyncFeedReadSucceeded();
//                else
//                    mListener.AsyncFeedReadFailed();
            }
        }.execute();
    }

    public String getTitle() {
            return mTitle;
    }

    public String getDate() {
        return mDate;
    }

    public String getDescription() {
//        if (!mDescriptionImagesAlreadyScaled)
//            scaleDescriptionImages();

            return mDescription;
    }

    private void scaleDescriptionImages() {
        //String unescapedDescription = Html.fromHtml(mDescription);
        
        Log.d(TAG, "[scaleDescriptionImages] description before scale: " + mDescription);

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setCoalescing(true);
            dbf.setValidating(false);
            dbf.setNamespaceAware(false);
            DocumentBuilder db = dbf.newDocumentBuilder();

            /* The following parse call takes ten seconds on a fast phone.
                XMLPullParser is said to be a faster way to go.
                Sample code here: http://groups.google.com/group/android-developers/msg/ddc6a8e83963a6b5
                Another thread: http://stackoverflow.com/questions/4958973/3rd-party-android-xml-parser */
            mDescription = "<div id=\"root_element_required_for_dom_parser\">" + mDescription + "</div>\0";
            InputStream stream = new ByteArrayInputStream(mDescription.getBytes("UTF-8"));
            Document d = db.parse(stream);

            Element root = (Element) d.getFirstChild();

            NodeList nl = root.getElementsByTagName("img");
            for (int x=0; x<nl.getLength(); x++) {
                Element image = (Element) nl.item(x);
                String style = image.getAttribute("style");

                Log.d(TAG, "[scaleDescriptionImages] style attribute: " + style);
            }

//            TransformerFactory tf = TransformerFactory.newInstance();
//            Transformer t = tf.newTransformer();
//            t.setOutputProperty(OutputKeys.INDENT, "yes");
//            StringWriter sw = new StringWriter();
//            t.transform(new DOMSource(d), new StreamResult(sw));
//            System.out.println(sw.toString());

//            String descriptionText = "";
//            NodeList nl2 = root.getChildNodes();
//            for (int x=0; x<nl2.getLength(); x++) {
//                String nodeValue = nl2.item(x).getNodeValue();
//                descriptionText += nodeValue;
//            }

//            mDescription = root.getTextContent();//null;//sw.toString();//descriptionText;

            //mDescription = d.getTextContent();//d.toString();
            //mDescriptionImagesAlreadyScaled = true;

            Log.d(TAG, "[scaleDescriptionImages] description after scale: " + mDescription);
        }
        catch (MalformedURLException e) { Log.d("scaleDescriptionImages", "MalformedURLException" + e.getLocalizedMessage()); }
        catch (IOException e) { Log.d("scaleDescriptionImages", "IOException" + e.getLocalizedMessage()); }
        catch (ParserConfigurationException e) { Log.d("scaleDescriptionImages", "ParserConfigurationException" + e.getLocalizedMessage()); }
        catch (SAXException e) { Log.d("scaleDescriptionImages", "SAXException" + e.getLocalizedMessage());  throw new RuntimeException(e);}
        catch (NullPointerException e) { Log.d("scaleDescriptionImages", "NullPointerException" + e.getLocalizedMessage()); }
        //catch (TransformerException e) { Log.d("scaleDescriptionImages", "NullPointerException" + e.getLocalizedMessage()); }
    }

    public String getPlainText() {
        return mPlainText;
    }

    public String getLink() {
        return mLink;
    }

    public ArrayList<String> getImageUrls() {
        return mImageUrls;
    }

//    public ScaleDrawable getImage() {
//    public BitmapDrawable getImage() {
    public Bitmap getImage() {
        return mImage;
    }

}
