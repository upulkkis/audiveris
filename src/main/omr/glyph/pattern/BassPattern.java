//----------------------------------------------------------------------------//
//                                                                            //
//                           B a s s P a t t e r n                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.CompoundBuilder;
import omr.glyph.CompoundBuilder.CompoundAdapter;
import omr.glyph.Evaluation;
import omr.glyph.Shape;
import omr.glyph.ShapeRange;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.sheet.Scale;
import omr.sheet.Scale.InterlineFraction;
import omr.sheet.StaffInfo;
import omr.sheet.SystemInfo;

import omr.util.Implement;

/**
 * Class {@code BassPattern} checks for segmented bass clefs, in the
 * neighborhood of typical vertical two-dot patterns
 *
 * @author Hervé Bitteur
 */
public class BassPattern
    extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BassPattern.class);

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // BassPattern //
    //-------------//
    /**
     * Creates a new BassPattern object.
     * @param system the containing system
     */
    public BassPattern (SystemInfo system)
    {
        super("Bass", system);
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // runPattern //
    //------------//
    @Implement(GlyphPattern.class)
    public int runPattern ()
    {
        int             successNb = 0;

        // Constants for clef verification
        final double    clefMaxDoubt = constants.clefMaxDoubt.getValue();
        final double    maxBassDotPitchDy = constants.maxBassDotPitchDy.getValue();
        final double    maxBassDotDx = scale.toPixels(constants.maxBassDotDx);

        // Specific adapter definition for bass clefs
        CompoundAdapter bassAdapter = new BassAdapter(system, clefMaxDoubt);

        for (Glyph top : system.getGlyphs()) {
            // Look for top dot
            if ((top.getShape() != Shape.DOT) ||
                (Math.abs(top.getPitchPosition() - -3) > maxBassDotPitchDy)) {
                continue;
            }

            int       topX = top.getCentroid().x;
            StaffInfo topStaff = system.getStaffAtY(top.getCentroid().y);

            // Look for bottom dot right underneath, and in the same staff
            for (Glyph bot : system.getGlyphs()) {
                if ((bot.getShape() != Shape.DOT) ||
                    (Math.abs(bot.getPitchPosition() - -1) > maxBassDotPitchDy)) {
                    continue;
                }

                if (Math.abs(bot.getCentroid().x - topX) > maxBassDotDx) {
                    continue;
                }

                if (system.getStaffAtY(bot.getCentroid().y) != topStaff) {
                    continue;
                }

                // Here we have a couple
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Got bass dots #" + top.getId() + " & #" + bot.getId());
                }

                Glyph compound = system.getCompoundBuilder()
                                       .buildCompound(
                    top,
                    system.getGlyphs(),
                    bassAdapter);

                if (compound != null) {
                    successNb++;
                }
            }
        }

        return successNb;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-------------//
    // BassAdapter //
    //-------------//
    /**
     * This is the compound adapter meant to build bass clefs
     */
    private class BassAdapter
        extends CompoundBuilder.TopShapeAdapter
    {
        //~ Constructors -------------------------------------------------------

        public BassAdapter (SystemInfo system,
                            double     maxDoubt)
        {
            super(system, maxDoubt, ShapeRange.BassClefs);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public boolean isCandidateSuitable (Glyph glyph)
        {
            return !glyph.isManualShape() ||
                   ShapeRange.BassClefs.contains(glyph.getShape());
        }

        @Override
        public PixelRectangle getIntersectionBox ()
        {
            if (seed == null) {
                throw new NullPointerException(
                    "Compound seed has not been set");
            }

            PixelRectangle pixRect = new PixelRectangle(seed.getCentroid());
            pixRect.add(
                new PixelPoint(
                    pixRect.x - scale.toPixels(new InterlineFraction(2)),
                    pixRect.y + scale.toPixels(new InterlineFraction(3))));

            return pixRect;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction   maxBassDotDx = new Scale.Fraction(
            0.25,
            "Tolerance on Bass dot abscissae");
        Constant.Double  maxBassDotPitchDy = new Constant.Double(
            "pitch",
            0.5,
            "Ordinate tolerance on a Bass dot pitch position");
        Evaluation.Doubt clefMaxDoubt = new Evaluation.Doubt(
            3d,
            "Maximum doubt for clef verification");
    }
}