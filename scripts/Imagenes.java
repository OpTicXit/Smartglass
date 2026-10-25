import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * U2 · Imagenes livianas para la web (solo JDK, sin dependencias).
 *
 *   java scripts/Imagenes.java
 *
 * - hero-mountains.jpg: era un PNG de 2,2 MB con extension .jpg; se guarda como JPEG real.
 *   El original queda en scripts/originales/ para poder repetir el proceso.
 * - logo-icono-sm.png: copia del logotipo a 128 px de alto para barras, pie y favicon (se
 *   muestra a 28-34 px). El logotipo original no se toca: la factura y la ficha lo siguen usando.
 */
public class Imagenes {

    static final File IMG = new File("src/main/resources/static/img");
    static final File ORIGINALES = new File("scripts/originales");

    public static void main(String[] args) throws Exception {
        ORIGINALES.mkdirs();
        File hero = new File(IMG, "hero-mountains.jpg"), heroOriginal = new File(ORIGINALES, "hero-mountains.png");
        if (!heroOriginal.exists()) java.nio.file.Files.copy(hero.toPath(), heroOriginal.toPath());
        BufferedImage fuente = ImageIO.read(heroOriginal);
        BufferedImage rgb = new BufferedImage(fuente.getWidth(), fuente.getHeight(), BufferedImage.TYPE_INT_RGB);
        rgb.createGraphics().drawImage(fuente, 0, 0, null);
        jpeg(rgb, hero, 0.82f);

        BufferedImage logo = ImageIO.read(new File(IMG, "logo-icono.png"));
        ImageIO.write(reducir(logo, 128), "png", new File(IMG, "logo-icono-sm.png"));
        System.out.println("hero " + hero.length() + " B, logo-icono-sm " + new File(IMG, "logo-icono-sm.png").length() + " B");
    }

    /** Reduce a la mitad en pasos (bicubica) hasta la altura pedida: bordes nitidos sin escalones. */
    static BufferedImage reducir(BufferedImage img, int alto) {
        BufferedImage actual = img;
        int w = img.getWidth(), h = img.getHeight();
        while (h > alto) {
            int nh = Math.max(alto, h / 2), nw = Math.round((float) w * nh / h);
            BufferedImage sig = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sig.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(actual, 0, 0, nw, nh, null);
            g.dispose();
            actual = sig; w = nw; h = nh;
        }
        return actual;
    }

    static void jpeg(BufferedImage img, File destino, float calidad) throws Exception {
        ImageWriter w = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam p = w.getDefaultWriteParam();
        p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        p.setCompressionQuality(calidad);
        destino.delete();   // el flujo de ImageIO no trunca: un archivo previo mas grande dejaria basura al final
        try (ImageOutputStream out = ImageIO.createImageOutputStream(destino)) {
            w.setOutput(out);
            w.write(null, new IIOImage(img, null, null), p);
        }
        w.dispose();
    }
}
