import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;

import javax.imageio.ImageIO;

/** Deterministically rasterizes the indexed Russian sign-message catalog. */
public final class SignAtlasGenerator {
	private static final int WIDTH = 96;
	private static final int ROW_HEIGHT = 35;
	private static final int MESSAGE_COUNT = 87;
	private static final int HORIZONTAL_MARGIN = 3;

	private SignAtlasGenerator() {
	}

	public static void main(String[] args) throws IOException, FontFormatException {
		if (args.length != 2) throw new IllegalArgumentException("Usage: SignAtlasGenerator FONT_FILE OUTPUT_PNG");
		Font baseFont = Font.createFont(Font.TRUETYPE_FONT, Path.of(args[0]).toFile());
		BufferedImage atlas = new BufferedImage(WIDTH, ROW_HEIGHT * MESSAGE_COUNT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = atlas.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
		graphics.setColor(Color.BLACK);

		int[] fontSizes = new int[11];
		int widestLine = 0;
		try (BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
			for (int index = 0; index < MESSAGE_COUNT; index++) {
				String record = input.readLine();
				if (record == null) throw new IllegalArgumentException("Missing message " + index);
				String[] fields = record.split("\\t", 2);
				if (fields.length != 2 || Integer.parseInt(fields[0]) != index) {
					throw new IllegalArgumentException("Unexpected message index at row " + index);
				}
				String[] lines = new String(Base64.getDecoder().decode(fields[1]), StandardCharsets.UTF_8).split("\\n", -1);
				if (lines.length < 1 || lines.length > 4) throw new IllegalArgumentException("Invalid line count at " + index);

				Font chosenFont = null;
				FontMetrics metrics = null;
				int lineStep = 0;
				for (int size = 10; size >= 7; size--) {
					Font candidate = baseFont.deriveFont(Font.PLAIN, (float) size);
					graphics.setFont(candidate);
					FontMetrics candidateMetrics = graphics.getFontMetrics(candidate);
					int candidateStep = Math.max(8, size + 1);
					int textHeight = (lines.length - 1) * candidateStep + candidateMetrics.getAscent() + candidateMetrics.getDescent();
					boolean fits = textHeight <= ROW_HEIGHT
						&& java.util.Arrays.stream(lines).allMatch(line -> !line.isBlank()
							&& candidate.canDisplayUpTo(line) < 0
							&& candidateMetrics.stringWidth(line) <= WIDTH - 2 * HORIZONTAL_MARGIN);
					if (fits) {
						chosenFont = candidate;
						metrics = candidateMetrics;
						lineStep = candidateStep;
						break;
					}
				}
				if (chosenFont == null) throw new IllegalArgumentException("Russian sign message " + index + " does not fit in 96x35 pixels");
				graphics.setFont(chosenFont);
				fontSizes[chosenFont.getSize()]++;
				int textHeight = (lines.length - 1) * lineStep + metrics.getAscent() + metrics.getDescent();
				int baseline = index * ROW_HEIGHT + (ROW_HEIGHT - textHeight) / 2 + metrics.getAscent();
				for (String line : lines) {
					int width = metrics.stringWidth(line);
					widestLine = Math.max(widestLine, width);
					graphics.drawString(line, (WIDTH - width) / 2, baseline);
					baseline += lineStep;
				}
			}
			if (input.readLine() != null) throw new IllegalArgumentException("The catalog contains more than 87 entries");
		} finally {
			graphics.dispose();
		}

		if (!ImageIO.write(atlas, "png", Path.of(args[1]).toFile())) throw new IOException("No PNG writer is available");
		System.out.printf("Rendered %d Russian signs at %dx%d; widest line %d pixels; font sizes: 7=%d, 8=%d, 9=%d, 10=%d%n",
			MESSAGE_COUNT, WIDTH, atlas.getHeight(), widestLine, fontSizes[7], fontSizes[8], fontSizes[9], fontSizes[10]);
	}
}
