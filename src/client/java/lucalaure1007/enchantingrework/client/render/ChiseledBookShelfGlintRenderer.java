package lucalaure1007.enchantingrework.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the enchantment glint over enchanted books in chiseled bookshelves. The block model already draws the
 * books; this redraws each enchanted book's face with the same texture plus glint, a hair in front of the block.
 */
// TextureAtlas.LOCATION_BLOCKS is deprecated in 26.3 but is still what vanilla's own block sprite ids use.
@SuppressWarnings("deprecation")
public class ChiseledBookShelfGlintRenderer implements BlockEntityRenderer<ChiseledBookShelfBlockEntity, ChiseledBookShelfGlintRenderer.State> {
	private static final SpriteId BOOKS = new SpriteId(TextureAtlas.LOCATION_BLOCKS, Identifier.withDefaultNamespace("block/chiseled_bookshelf_occupied"));
	/** Slot areas on the north face in model pixels {x0, y0, x1, y1}, matching the vanilla slot models. */
	private static final int[][] SLOT_AREAS = {
		{10, 8, 16, 16}, {5, 8, 10, 16}, {0, 8, 5, 16},
		{10, 0, 16, 8}, {5, 0, 10, 8}, {0, 0, 5, 8}
	};
	private static final float OFFSET = 0.001F;

	private final TextureAtlasSprite sprite;

	public ChiseledBookShelfGlintRenderer(BlockEntityRendererProvider.Context context) {
		this.sprite = context.sprites().get(BOOKS);
	}

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		int frontLight;
		final boolean[] glinting = new boolean[SLOT_AREAS.length];
		boolean any;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(
		ChiseledBookShelfBlockEntity shelf, State state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
	) {
		BlockEntityRenderer.super.extractRenderState(shelf, state, partialTicks, cameraPosition, breakProgress);
		state.facing = shelf.getBlockState().getValue(ChiseledBookShelfBlock.FACING);
		// The shelf itself is solid and dark inside; light the books with the light in front of them.
		state.frontLight = shelf.getLevel() != null
			? LightCoordsUtil.getLightCoords(shelf.getLevel(), shelf.getBlockPos().relative(state.facing))
			: state.lightCoords;
		state.any = false;
		for (int slot = 0; slot < SLOT_AREAS.length; slot++) {
			boolean glint = slot < shelf.getContainerSize() && shelf.getItem(slot).hasFoil();
			state.glinting[slot] = glint;
			state.any |= glint;
		}
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.any) {
			return;
		}

		poseStack.pushPose();
		// Slot models face north; turn them to the shelf's facing around the block's centre.
		poseStack.translate(0.5F, 0.0F, 0.5F);
		poseStack.rotateDegrees(Axis.YP, 180.0F - state.facing.toYRot());
		poseStack.translate(-0.5F, 0.0F, -0.5F);

		collector.submitCustomGeometry(poseStack, RenderTypes.entitySolidGlint(TextureAtlas.LOCATION_BLOCKS), (pose, buffer) -> {
			for (int slot = 0; slot < SLOT_AREAS.length; slot++) {
				if (!state.glinting[slot]) {
					continue;
				}

				int[] a = SLOT_AREAS[slot];
				float x0 = a[0] / 16.0F, y0 = a[1] / 16.0F, x1 = a[2] / 16.0F, y1 = a[3] / 16.0F;
				// North face seen from outside: u runs right-to-left along x, v top-to-bottom along y.
				float u0 = this.sprite.getU((16 - a[2]) / 16.0F), u1 = this.sprite.getU((16 - a[0]) / 16.0F);
				float v0 = this.sprite.getV((16 - a[3]) / 16.0F), v1 = this.sprite.getV((16 - a[1]) / 16.0F);
				float z = -OFFSET;

				vertex(buffer, pose, x0, y0, z, u1, v1, state.frontLight);
				vertex(buffer, pose, x0, y1, z, u1, v0, state.frontLight);
				vertex(buffer, pose, x1, y1, z, u0, v0, state.frontLight);
				vertex(buffer, pose, x1, y0, z, u0, v1, state.frontLight);
			}
		});
		poseStack.popPose();
	}

	private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light) {
		buffer.addVertex(pose, x, y, z)
			.setColor(-1)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light)
			.setNormal(pose, 0.0F, 0.0F, -1.0F);
	}
}
