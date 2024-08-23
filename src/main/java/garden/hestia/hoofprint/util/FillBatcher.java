package garden.hestia.hoofprint.util;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.ColorHelper;
import org.joml.Matrix4f;

public class FillBatcher implements AutoCloseable {
	private final DrawContext context;
	private final Matrix4f matrix4f;
	private final VertexConsumer vertexConsumer;

	public FillBatcher(DrawContext context) {
		this.context = context;
		this.matrix4f = context.getMatrices().peek().getPositionMatrix();
		this.vertexConsumer = context.getVertexConsumers().getBuffer(RenderLayer.getGui());
	}

	public void add(int x1, int y1, int x2, int y2, int z, int color) {
		float f = (float) ColorHelper.Argb.getAlpha(color) / 255.0F;
		float g = (float) ColorHelper.Argb.getRed(color) / 255.0F;
		float h = (float) ColorHelper.Argb.getGreen(color) / 255.0F;
		float j = (float) ColorHelper.Argb.getBlue(color) / 255.0F;
		vertexConsumer.vertex(matrix4f, (float) x1, (float) y1, (float) z).color(g, h, j, f);
		vertexConsumer.vertex(matrix4f, (float) x1, (float) y2, (float) z).color(g, h, j, f);
		vertexConsumer.vertex(matrix4f, (float) x2, (float) y2, (float) z).color(g, h, j, f);
		vertexConsumer.vertex(matrix4f, (float) x2, (float) y1, (float) z).color(g, h, j, f);
	}

	@Override
	public void close() {
		context.draw();
	}
}
