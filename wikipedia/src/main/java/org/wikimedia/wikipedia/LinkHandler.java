package org.wikimedia.wikipedia;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import com.squareup.otto.Bus;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikimedia.wikipedia.events.NewWikiPageNavigationEvent;

/**
 * Handles any html links coming from a {@link PageViewFragment}
 */
public class LinkHandler implements CommunicationBridge.JSEventListener {
    private final Context context;
    private final CommunicationBridge bridge;
    private final Bus bus;
    private final Site currentSite;

    public LinkHandler(Context context, CommunicationBridge bridge, Site currentSite) {
        this.context = context;
        this.bridge = bridge;
        this.bus = ((WikipediaApp)context.getApplicationContext()).getBus();
        this.currentSite = currentSite;

        this.bridge.addListener("linkClicked", this);
    }

    private void handleExternalLink(Uri uri) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
        context.startActivity(intent);
    }

    @Override
    public void onMessage(String messageType, JSONObject messagePayload) {
        try {
            String href = messagePayload.getString("href");
            if (href.startsWith("//")) {
                // That's a protocol specific link! Make it https!
                href = "https:" + href;
            }
            Log.d("Wikipedia", "Link clicked was " + href);
            if (href.startsWith("/wiki/")) {
                // TODO: Handle fragments
                bus.post(new NewWikiPageNavigationEvent(currentSite.titleForInternalLink(href)));
            } else {
                Uri uri = Uri.parse(href);
                String authority = uri.getAuthority();
                if(authority != null && Site.isSupportedSite(authority)) {
                    Site site = new Site(authority);
                    //TODO: Handle fragments
                    bus.post(new NewWikiPageNavigationEvent(site.titleForInternalLink(uri.getPath())));
                } else {
                    handleExternalLink(uri);
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}