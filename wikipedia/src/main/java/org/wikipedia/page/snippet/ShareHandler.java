package org.wikipedia.page.snippet;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

import org.wikipedia.PageTitle;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.BottomDialog;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageViewFragmentInternal;
import org.wikipedia.util.ShareUtils;

import static org.wikipedia.page.snippet.ShareAFactFunnel.ShareMode;

/**
 * Let user choose between sharing as text or as image.
 */
public class ShareHandler {
    private final PageActivity activity;
    private Dialog shareDialog;
    private ShareAFactFunnel funnel;

    protected PageActivity getActivity() {
        return activity;
    }

    protected ShareAFactFunnel getFunnel() {
        return funnel;
    }

    protected void createFunnel() {
        WikipediaApp app = (WikipediaApp) getActivity().getApplicationContext();
        final Page page = getActivity().getCurPageFragment().getPage();
        final PageProperties pageProperties = page.getPageProperties();
        funnel = new ShareAFactFunnel(app, page.getTitle(), pageProperties.getPageId(),
                pageProperties.getRevisionId());
    }

    public ShareHandler(PageActivity activity) {
        this.activity = activity;
    }

    public void onDestroy() {
        if (shareDialog != null) {
            shareDialog.dismiss();
            shareDialog = null;
        }
    }

    /** Call #setFunnel before #shareSnippet. */
    public void shareSnippet(CharSequence input, boolean preferUrl) {
        final PageViewFragmentInternal curPageFragment = activity.getCurPageFragment();
        if (curPageFragment == null) {
            return;
        }

        String selectedText = sanitizeText(input.toString());
        final int minTextSnippetLength = 2;
        final PageTitle title = curPageFragment.getTitle();
        if (selectedText.length() >= minTextSnippetLength) {
            String introText = activity.getString(R.string.snippet_share_intro,
                    title.getDisplayText(),
                    title.getCanonicalUri() + "?wprov=sfia1");
            // For wprov=sfia1: see https://www.mediawiki.org/wiki/Provenance;
            // introText is only used for image share

            Bitmap resultBitmap = new SnippetImage(activity,
                    curPageFragment.getLeadImageBitmap(),
                    curPageFragment.getLeadImageFocusY(),
                    title.getDisplayText(),
                    curPageFragment.getPage().getPageProperties().isMainPage() ? "" : title.getDescription(),
                    selectedText).createImage();
            if (shareDialog != null) {
                shareDialog.dismiss();
            }
            shareDialog = new PreviewDialog(activity, resultBitmap, title.getDisplayText(), introText,
                    selectedText, preferUrl ? title.getCanonicalUri() : selectedText, funnel);
            shareDialog.show();
        } else {
            // only share the URL
            ShareUtils.shareText(activity, title.getDisplayText(), title.getCanonicalUri());
        }
    }

    private static String sanitizeText(String selectedText) {
        return selectedText.replaceAll("\\[\\d+\\]", "") // [1]
                .replaceAll("\\(\\s*;\\s*", "\\(") // (; -> (    hacky way for IPA remnants
                .replaceAll("\\s{2,}", " ")
                .trim();
    }
}


/**
 * A dialog to be displayed before sharing with two action buttons:
 * "Share as image", "Share as text".
 */
class PreviewDialog extends BottomDialog {
    private boolean completed = false;

    public PreviewDialog(final PageActivity activity, final Bitmap resultBitmap,
                         final String title, final String introText, final String selectedText,
                         final String alternativeText, final ShareAFactFunnel funnel) {
        super(activity, R.layout.dialog_share_preview);
        ImageView previewImage = (ImageView) getDialogLayout().findViewById(R.id.preview_img);
        previewImage.setImageBitmap(resultBitmap);
        getDialogLayout().findViewById(R.id.share_as_image_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ShareUtils.shareImage(activity, resultBitmap, title, title, introText, false);
                        funnel.logShareIntent(selectedText, ShareMode.image);
                        completed = true;
                    }
                });
        getDialogLayout().findViewById(R.id.share_as_text_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ShareUtils.shareText(activity, title, alternativeText);
                        funnel.logShareIntent(alternativeText, ShareMode.text);
                        completed = true;
                    }
                });
        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                resultBitmap.recycle();
                if (!completed) {
                    funnel.logAbandoned(alternativeText);
                }
            }
        });
    }
}
