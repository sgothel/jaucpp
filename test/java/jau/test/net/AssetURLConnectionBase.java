package jau.test.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URLConnection;

import org.jau.io.IOUtil;
import org.jau.net.AssetURLConnection;
import org.jau.net.Uri;
import org.jau.sys.AndroidVersion;
import org.junit.Assert;

import jau.test.junit.util.JunitTracer;

public abstract class AssetURLConnectionBase extends JunitTracer {

    /** In jaulib_base.jar */
    protected static final String test_asset_rt_url      = "asset:jau/info.txt";
    protected static final String test_asset_rt_entry    = "jau/info.txt";

    protected static final String test_asset_rt2_url     = "asset:/jau/info.txt";

    /** In gluegen.test.jar */
    protected static final String test_asset_test1_url   = "asset:jau-test/info.txt";
    protected static final String test_asset_test1_entry = "jau-test/info.txt";
    protected static final Uri.Encoded test_asset_test2_rel   = Uri.Encoded.cast("data/AssetURLConnectionTest.txt");
    protected static final String test_asset_test2a_url  = "asset:jau/test/net/data/AssetURLConnectionTest.txt";
    protected static final String test_asset_test2b_url  = "asset:/jau/test/net/data/AssetURLConnectionTest.txt";
    protected static final String test_asset_test2_entry = "jau/test/net/data/AssetURLConnectionTest.txt";
    protected static final Uri.Encoded test_asset_test3_rel   = Uri.Encoded.cast("RelativeData.txt");
    protected static final String test_asset_test3a_url  = "asset:jau/test/net/data/RelativeData.txt";
    protected static final String test_asset_test3b_url  = "asset:/jau/test/net/data/RelativeData.txt";
    protected static final String test_asset_test3_entry = "jau/test/net/data/RelativeData.txt";
    protected static final Uri.Encoded test_asset_test4_rel   = Uri.Encoded.cast("../data2/RelativeData2.txt");
    protected static final String test_asset_test4a_url  = "asset:jau/test/net/data2/RelativeData2.txt";
    protected static final String test_asset_test4b_url  = "asset:/jau/test/net/data2/RelativeData2.txt";
    protected static final String test_asset_test4_entry = "jau/test/net/data2/RelativeData2.txt";

    protected static void testAssetConnection(final URLConnection c, final String entry_name) throws IOException {
        Assert.assertNotNull(c);
        if(c instanceof AssetURLConnection) {
            final AssetURLConnection ac = (AssetURLConnection) c;
            Assert.assertEquals(entry_name, ac.getEntryName());
        } else if(c instanceof JarURLConnection) {
            final JarURLConnection jc = (JarURLConnection) c;
            if(AndroidVersion.isAvailable) {
                Assert.assertEquals("assets/"+entry_name, jc.getEntryName());
            } else {
                Assert.assertEquals(entry_name, jc.getEntryName());
            }
        }

        final BufferedReader reader = new BufferedReader(new InputStreamReader(c.getInputStream()));
        try {
            String line = null;
            int l = 0;
            while ((line = reader.readLine()) != null) {
                System.err.println(c.getURL()+":"+l+"> "+line);
                l++;
            }
        } finally {
            IOUtil.close(reader, false);
        }
    }
}