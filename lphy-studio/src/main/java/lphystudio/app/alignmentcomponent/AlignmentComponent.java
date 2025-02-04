package lphystudio.app.alignmentcomponent;

import jebl.evolution.sequences.SequenceType;
import lphy.base.evolution.alignment.Alignment;
import lphy.base.evolution.alignment.ErrorAlignment;
import lphy.base.evolution.likelihood.AbstractPhyloCTMC;
import lphy.base.evolution.likelihood.PhyloCTMC;
import lphy.base.evolution.tree.TimeTree;
import lphy.core.model.GenerativeDistribution;
import lphy.core.model.RandomVariable;
import lphy.core.model.Value;
import lphystudio.app.FontUtils;
import lphystudio.app.treecomponent.TimeTreeComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;

public class AlignmentComponent extends JComponent {
    public static boolean showErrorsIfAvailable = true;

    static Preferences preferences = Preferences.userNodeForPackage(AlignmentComponent.class);
    static Font taxaMinFont = FontUtils.MIN_FONT;

    Color[] colors;
//    Value<? extends Alignment> alignmentValue; // required to get the tree from PhyloCTMC
    Alignment alignment;
    Value<TimeTree> timeTree = null;

    int spacer = 5;
    int maxTaxaWidth = 0;

    private Font taxaFont = taxaMinFont;

//    private boolean isClamped;

    public AlignmentComponent(Value<? extends Alignment> av) {
//        this.alignmentValue = av;
//        this.isClamped = av.isClamped();
        this.alignment = av.value();
        SequenceType sequenceType = alignment.getSequenceType();
        this.colors = ColourPalette.getCanonicalStateColours(sequenceType);

        if (av instanceof RandomVariable) {
            GenerativeDistribution gen = ((RandomVariable)av).getGenerativeDistribution();
            if (gen instanceof PhyloCTMC) {
                timeTree = ((AbstractPhyloCTMC) gen).getParams().get("tree");

                // validate
                String[] taxaNames = alignment.getTaxaNames();
                Arrays.sort(taxaNames);
                String[] taxaNames2 = timeTree.value().getTaxaNames();
                Arrays.sort(taxaNames2);
                if (!Arrays.equals(taxaNames, taxaNames2))
                    throw new IllegalArgumentException("Alignment " + av.getId() +
                            " has different taxa names to tree " + timeTree.getId() + " !");
            }
        }

//        addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//
//                if (e.getButton() == MouseEvent.BUTTON1) {
//                    setShowTreeInAlignmentViewerIfAvailable(!getShowTreeInAlignmentViewerIfAvailable());
//                } else {
//                    showErrorsIfAvailable = !showErrorsIfAvailable;
//                }
//                repaint();
//            }
//        });

        computeMinMaxSize();

        preferences.addPreferenceChangeListener(evt -> computeMinMaxSize());
    }

    private void computeMinMaxSize() {
        int maximumWidth = FontUtils.getMaxWidthWithinScreen(alignment.nchar());
        int maximumHeight = FontUtils.getMaxHeightWithinScreen(alignment.ntaxa());
        int minimumHeight = FontUtils.MIN_FONT_SIZE * alignment.ntaxa();

        maxTaxaWidth = getMaxStringWidth(alignment.getTaxa().getTaxaNames(), getFontMetrics(taxaMinFont));
        if (maxTaxaWidth < 50) maxTaxaWidth = 50;

        int minimumWidth = maxTaxaWidth + Math.max(alignment.nchar(), FontUtils.MIN_FONT_SIZE);

        if (isShowingTree()) minimumWidth += maxTaxaWidth;

        setMaximumSize(new Dimension(maximumWidth, maximumHeight));
        setMinimumSize(new Dimension(minimumWidth, minimumHeight));
        repaint();
    }

    private int getMaxStringWidth(String[] strings, FontMetrics fontMetrics) {
        int maxStringWidth = 0;
        for (String str : strings) {
            int stringWidth = fontMetrics.stringWidth(str);
            if (stringWidth > maxStringWidth) maxStringWidth = stringWidth;
        }
        return maxStringWidth;
    }

    // for CharSetAlignment
    public AlignmentComponent() { }

    // for other JComponent
    public Font getTaxaFont() {
        return taxaFont;
    }

    public Alignment getAlignment() {
        return Objects.requireNonNull(alignment);
    }

    public static boolean getShowTreeInAlignmentViewerIfAvailable() {
        return preferences.getBoolean("showTreeInAlignmentViewerIfAvailable", true);
    }

    public static void setShowTreeInAlignmentViewerIfAvailable(boolean showTreeInAlignmentViewerIfAvailable) {
        preferences.putBoolean("showTreeInAlignmentViewerIfAvailable", showTreeInAlignmentViewerIfAvailable);
    }

    public void paintComponent(Graphics g) {

        Insets insets = getInsets();
        g.translate(insets.left, insets.top);
        int width = getWidth() - insets.left - insets.right;
        int height = getHeight() - insets.top - insets.bottom;

        Graphics2D g2d = (Graphics2D) g;

        int xdelta = 0;

        double h = height / (double) alignment.ntaxa();
        taxaFont = FontUtils.deriveFont(h);
        g.setFont(taxaFont);

        // alignment taxa order is not same as the order to draw tips in the tree
        // if isShowingTree() then extract the order to draw tips into taxa2Draw,
        // otherwise use the alignment taxa order
        String[] taxa2Draw = alignment.getTaxaNames();
        if (isShowingTree()) {

            int ytrans = (int)Math.round(h/2);

            int treeHeight = (int)Math.round(height - h);
            g.translate(0, ytrans);

            TimeTreeComponent treeComponent = new TimeTreeComponent(timeTree.value());
            treeComponent.setBorder(BorderFactory.createEmptyBorder(1,1,1,0));
            treeComponent.setSize(maxTaxaWidth*2, treeHeight);
            treeComponent.paintComponent(g);
            width -= 2.0*maxTaxaWidth;
            xdelta = 2*maxTaxaWidth;
            g.translate(0, -ytrans);

            taxa2Draw = treeComponent.getTips2Draw();
            assert taxa2Draw.length == alignment.ntaxa();
        }

        int maxWidth = 0;
        int[] sWidth = new int[taxa2Draw.length];
        // compute max width of taxa names
        for (int i = 0; i < taxa2Draw.length; i++) {
            sWidth[i] = g.getFontMetrics().stringWidth(taxa2Draw[i]);
            if (sWidth[i] > maxWidth) maxWidth = sWidth[i];
        }

        double w = (width - maxWidth - spacer) / (double) alignment.nchar();

        int ascent = g.getFontMetrics().getAscent();
        double ydelta = (h - ascent) / 2.0 + ascent;

        List<String> algTaxaNames = Arrays.asList(alignment.taxaNames());
        // draw alignment
        for (int i = 0; i < taxa2Draw.length; i++) {
            double y = i * h;

            if (!isShowingTree()) {
                g.setColor(Color.black);
                g.drawString(taxa2Draw[i], maxWidth-sWidth[i]+xdelta,(int)Math.round(y+ydelta));
            }

            for (int j = 0; j < alignment.nchar(); j++) {
                // need to adjust id according to what taxa order is used.
                int adjustedId = algTaxaNames.indexOf(taxa2Draw[i]);

                int state = alignment.getState(adjustedId, j);
//                int col = SequenceTypeFactory.getColourIndex(state, alignment.getSequenceType());
                Color c = ColourPalette.getColour(state, colors);

                if (alignment instanceof ErrorAlignment && showErrorsIfAvailable
                        && ((ErrorAlignment)alignment).isError(adjustedId,j)) {
                    c = new Color(255-c.getRed(), 255-c.getGreen(), 255-c.getBlue());
                }

                g.setColor(c);

                Rectangle2D rect2D = new Rectangle2D.Double(j * w + xdelta + maxWidth + spacer, y, w, h * 0.95);

                g2d.fill(rect2D);
            }
        }
        g.translate(-insets.left, -insets.top);

    }

    boolean isShowingTree() {
//        if (isClamped) return false;
        return getShowTreeInAlignmentViewerIfAvailable() && timeTree != null;
    }
}
