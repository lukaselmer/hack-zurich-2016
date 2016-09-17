package ch.renuo.hackzurich2016.qrcode;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import ch.renuo.hackzurich2016.models.Household;

public class QRcodeInterface {
    public static final int QRCODE_DIMENSION = 300;

    public static Bitmap generateBitmap(Household household) throws Exception {
        BitMatrix matrix = generateBitMatrix(household);

        Bitmap bitmap = Bitmap.createBitmap(QRCODE_DIMENSION, QRCODE_DIMENSION, Bitmap.Config.ARGB_8888);
        for (int i = 0; i < QRCODE_DIMENSION; i++)
            for (int j = 0; j < QRCODE_DIMENSION; j++)
                bitmap.setPixel(i, j, matrix.get(i, j) ? Color.BLACK : Color.WHITE);

        return bitmap;
    }

    public static BitMatrix generateBitMatrix(Household household) throws WriterException {
        String qrCodeContent = household.getId().toString();
        return new QRCodeWriter().encode(qrCodeContent, BarcodeFormat.QR_CODE, QRCODE_DIMENSION, QRCODE_DIMENSION);
    }
}
