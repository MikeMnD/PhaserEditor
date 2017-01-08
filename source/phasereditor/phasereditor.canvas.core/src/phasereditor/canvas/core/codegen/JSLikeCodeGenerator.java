// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.canvas.core.codegen;

import static java.lang.String.format;

import java.util.List;
import java.util.function.Function;

import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.canvas.core.AnimationModel;
import phasereditor.canvas.core.ArcadeBodyModel;
import phasereditor.canvas.core.AssetSpriteModel;
import phasereditor.canvas.core.AtlasSpriteModel;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.BaseSpriteModel;
import phasereditor.canvas.core.BodyModel;
import phasereditor.canvas.core.ButtonSpriteModel;
import phasereditor.canvas.core.CircleArcadeBodyModel;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.ImageSpriteModel;
import phasereditor.canvas.core.PhysicsBodyType;
import phasereditor.canvas.core.PhysicsSortDirection;
import phasereditor.canvas.core.RectArcadeBodyModel;
import phasereditor.canvas.core.SpritesheetSpriteModel;
import phasereditor.canvas.core.TileSpriteModel;
import phasereditor.canvas.core.WorldModel;
import phasereditor.lic.LicCore;

/**
 * @author arian
 *
 */
public abstract class JSLikeCodeGenerator implements ICodeGenerator {

	protected static final String YOU_CAN_INSERT_CODE_HERE = "\n\n// you can insert code here\n\n";
	protected static final String PRE_INIT_CODE_BEGIN = "/* --- pre-init-begin --- */";
	protected static final String PRE_INIT_CODE_END = "/* --- pre-init-end --- */";
	protected static final String POST_INIT_CODE_BEGIN = "/* --- post-init-begin --- */";
	protected static final String POST_INIT_CODE_END = "/* --- post-init-end --- */";
	protected static final String END_GENERATED_CODE = "/* --- end generated code --- */";

	@Override
	public String generate(WorldModel model, String replace) {
		String tabs1 = tabs(1);

		String preInit = YOU_CAN_INSERT_CODE_HERE;
		String postInit = YOU_CAN_INSERT_CODE_HERE;
		String postGen = YOU_CAN_INSERT_CODE_HERE;

		if (replace != null) {
			int i;
			int j;
			i = replace.indexOf(PRE_INIT_CODE_BEGIN);
			j = replace.indexOf(PRE_INIT_CODE_END);
			if (i != -1 && j != -1) {
				preInit = replace.substring(i + PRE_INIT_CODE_BEGIN.length(), j);
			}

			i = replace.indexOf(POST_INIT_CODE_BEGIN);
			j = replace.indexOf(POST_INIT_CODE_END);
			if (i != -1 && j != -1) {
				postInit = replace.substring(i + POST_INIT_CODE_BEGIN.length(), j);
			}

			i = replace.indexOf(END_GENERATED_CODE);
			if (i != -1) {
				postGen = replace.substring(i + END_GENERATED_CODE.length());
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append("// Generated by " + LicCore.PRODUCT_NAME + "\n\n");
		String classname = model.getClassName();

		generateHeader(sb, preInit, classname);

		{
			int i = 0;
			int last = model.getChildren().size() - 1;
			for (BaseObjectModel child : model.getChildren()) {
				generateObjectCreate(1, sb, child);
				if (i < last) {
					sb.append("\n");
				}
				i++;
			}
		}

		sb.append("\n");

		// public fields
		StringBuilder pubs = new StringBuilder();
		model.walk(obj -> {
			if (!(obj instanceof WorldModel) && obj.isEditorGenerate()) {
				if (obj.isEditorPublic()) {
					String name = obj.getEditorName();
					String camel = getPublicFieldName(name);
					pubs.append(tabs1 + "this." + camel + " = " + name + ";\n");
				}

				if (obj instanceof BaseSpriteModel) {
					List<AnimationModel> anims = ((BaseSpriteModel) obj).getAnimations();
					for (AnimationModel anim : anims) {
						if (anim.isPublic()) {
							String animvar = getAnimationVarName(obj, anim);
							String name = getPublicFieldName(animvar);
							pubs.append(tabs1 + "this." + name + " = " + animvar + ";\n");
						}
					}
				}
			}
		});

		if (pubs.length() > 0) {
			sb.append(tabs1 + " // public fields\n\n");
			sb.append(pubs);
		}

		sb.append("\n");

		generateFooter(sb, postInit, postGen, classname);

		return sb.toString();
	}

	protected abstract void generateFooter(StringBuilder sb, String postInitUserCode, String postGenUserCode, String classname);

	protected abstract void generateHeader(StringBuilder sb, String preInitUserCode, String classname);

	protected static String getPublicFieldName(String name) {
		return "f" + name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	protected void generateObjectCreate(int indent, StringBuilder sb, BaseObjectModel model) {
		if (!model.isEditorGenerate()) {
			return;
		}

		if (model instanceof GroupModel) {
			generateGroup(indent, sb, (GroupModel) model);
		} else if (model instanceof BaseSpriteModel) {
			generateSprite(indent, sb, (BaseSpriteModel) model);
		}
	}

	private static void generateSprite(int indent, StringBuilder sb, BaseSpriteModel model) {

		// properties

		StringBuilder sbProps = new StringBuilder();

		generateDisplayProps(indent, sbProps, model);

		generateSpriteProps(indent, sbProps, model);

		if (model instanceof TileSpriteModel) {
			generateTileProps(indent, sbProps, (TileSpriteModel) model);
		}

		// create method

		sb.append(tabs(indent));
		String parVar = model.getParent().isWorldModel() ? "this" : model.getParent().getEditorName();
		if (sbProps.length() > 0 || model.isEditorPublic()) {
			sb.append("var " + model.getEditorName() + " = ");
		}
		sb.append("this.game.add.");

		if (model instanceof ImageSpriteModel) {
			ImageSpriteModel image = (ImageSpriteModel) model;
			sb.append("sprite(" + // sprite
					round(image.getX())// x
					+ ", " + round(image.getY()) // y
					+ ", '" + image.getAssetKey().getKey() + "'" // key
					+ ", null" // frame
					+ ", " + parVar // group
					+ ")");
		} else if (model instanceof SpritesheetSpriteModel || model instanceof AtlasSpriteModel) {
			AssetSpriteModel<?> sprite = (AssetSpriteModel<?>) model;
			IAssetKey frame = sprite.getAssetKey();
			String frameValue = frame instanceof SpritesheetAssetModel.FrameModel
					? Integer.toString(((SpritesheetAssetModel.FrameModel) frame).getIndex())
					: "'" + frame.getKey() + "'";
			sb.append("sprite(" + // sprite
					round(sprite.getX())// x
					+ ", " + round(sprite.getY()) // y
					+ ", '" + sprite.getAssetKey().getAsset().getKey() + "'" // key
					+ ", " + frameValue // frame
					+ ", " + parVar // group
					+ ")");
		} else if (model instanceof ButtonSpriteModel) {
			ButtonSpriteModel button = (ButtonSpriteModel) model;
			String outFrameKey;
			if (button.getAssetKey().getAsset() instanceof ImageAssetModel) {
				// buttons based on image do not have outFrames
				outFrameKey = "null";
			} else {
				outFrameKey = frameKey((IAssetFrameModel) button.getAssetKey());
			}

			sb.append("button(" + // sprite
					round(button.getX())// x
					+ ", " + round(button.getY()) // y
					+ ", '" + button.getAssetKey().getAsset().getKey() + "'" // key
					+ ", " + emptyStringToNull(button.getCallback()) // callback
					+ ", " + emptyStringToNull(button.getCallbackContext()) // context
					+ ", " + frameKey(button.getOverFrame())// overFrame
					+ ", " + outFrameKey// outFrame
					+ ", " + frameKey(button.getDownFrame())// downFrame
					+ ", " + frameKey(button.getUpFrame())// upFrame
					+ ", " + parVar // group
					+ ")");
		} else if (model instanceof TileSpriteModel) {
			TileSpriteModel tile = (TileSpriteModel) model;
			IAssetKey assetKey = tile.getAssetKey();
			String frame;
			if (assetKey instanceof SpritesheetAssetModel.FrameModel) {
				frame = assetKey.getKey();
			} else if (assetKey instanceof AtlasAssetModel.Frame) {
				frame = "'" + assetKey.getKey() + "'";
			} else {
				// like in case it is an image
				frame = "null";
			}

			sb.append("tileSprite(" + // sprite
					round(tile.getX())// x
					+ ", " + round(tile.getY()) // y
					+ ", " + round(tile.getWidth()) // width
					+ ", " + round(tile.getHeight()) // height
					+ ", '" + tile.getAssetKey().getAsset().getKey() + "'" // key
					+ ", " + frame// frame
					+ ", " + parVar // group
					+ ")");
		}
		sb.append(";\n");

		sb.append(sbProps);
	}

	private static void generateDisplayProps(int indent, StringBuilder sb, BaseObjectModel model) {
		String tabs = tabs(indent);
		String varname = model.getEditorName();

		if (model instanceof GroupModel) {
			if (model.getX() != 0 || model.getY() != 0) {
				sb.append(tabs + varname + ".position.setTo(" + round(model.getX()) + ", " + round(model.getY())
						+ ");\n");
			}
		}

		if (model.getAngle() != 0) {
			sb.append(tabs + varname + ".angle = " + model.getAngle() + ";\n");
		}

		if (model.getScaleX() != 1 || model.getScaleY() != 1) {
			sb.append(tabs + varname + ".scale.setTo(" + model.getScaleX() + ", " + model.getScaleY() + ");\n");
		}

		if (model.getPivotX() != 0 || model.getPivotY() != 0) {
			sb.append(tabs + varname + ".pivot.setTo(" + model.getPivotX() + ", " + model.getPivotY() + ");\n");
		}
	}

	private static void generateSpriteProps(int indent, StringBuilder sb, BaseSpriteModel model) {
		String tabs = tabs(indent);
		String varname = model.getEditorName();

		if (model.getAnchorX() != 0 || model.getAnchorY() != 0) {
			sb.append(tabs + varname + ".anchor.setTo(" + model.getAnchorX() + ", " + model.getAnchorY() + ");\n");
		}

		if (model.getTint() != null && !model.getTint().equals("0xffffff")) {
			sb.append(tabs + varname + ".tint = " + model.getTint() + ";\n");
		}

		if (!model.getAnimations().isEmpty()) {
			for (AnimationModel anim : model.getAnimations()) {
				sb.append(tabs);
				String animvar = null;
				if (anim.isPublic() || anim.isKillOnComplete()) {
					animvar = getAnimationVarName(model, anim);
					sb.append("var " + animvar + " = ");
				}

				sb.append(varname + ".animations.add(");

				sb.append("'" + anim.getName() + "', [");
				int i = 0;
				for (IAssetFrameModel frame : anim.getFrames()) {
					if (i++ > 0) {
						sb.append(", ");
					}
					if (frame instanceof SpritesheetAssetModel.FrameModel) {
						sb.append(frame.getKey());
					} else {
						sb.append("'" + frame.getKey() + "'");
					}
				}
				sb.append("], " + anim.getFrameRate() + ", " + anim.isLoop() + ");\n");

				if (anim.isKillOnComplete()) {
					sb.append(tabs + animvar + ".killOnComplete = true;\n");
				}
			}
		}

		generateBodyProps(indent, sb, model);

		// always generate data at the end, because it can use previous
		// properties.

		String data = model.getData();
		if (data != null && data.trim().length() > 0) {
			data = data.replace("$$", varname);
			data = data.replace("\n", "\n" + tabs + "\t");
			sb.append(tabs + varname + ".data = " + data + ";\n");
		}
	}

	private static String getAnimationVarName(BaseObjectModel obj, AnimationModel anim) {
		return obj.getEditorName() + "_" + anim.getName();
	}

	private static void generateBodyProps(int indent, StringBuilder sb, BaseSpriteModel model) {
		BodyModel body = model.getBody();
		if (body != null) {
			if (body instanceof ArcadeBodyModel) {
				generateArcadeBodyProps(indent, sb, model);
			}
		}
	}

	private static void generateArcadeBodyProps(int indent, StringBuilder sb, BaseSpriteModel model) {
		String tabs = tabs(indent);
		String varname = model.getEditorName();

		if (!model.getParent().isPhysicsGroup() || model.getParent().getPhysicsBodyType() != PhysicsBodyType.ARCADE) {
			sb.append(tabs + "this.game.physics.arcade.enable(" + varname + ");\n");
		}

		ArcadeBodyModel body = model.getArcadeBody();
		boolean hasOffset = body.getOffsetX() != 0 || body.getOffsetY() != 0;
		switch (body.getBodyType()) {
		case ARCADE_CIRCLE:
			CircleArcadeBodyModel circle = (CircleArcadeBodyModel) body;
			if (hasOffset) {
				sb.append(tabs + varname + ".body.setCircle(" + circle.getRadius() + ", " + circle.getOffsetX() + ", "
						+ circle.getOffsetY() + ");\n");
			} else {
				sb.append(tabs + varname + ".body.setCircle(" + circle.getRadius() + ");\n");
			}
			break;
		case ARCADE_RECT:
			RectArcadeBodyModel rect = (RectArcadeBodyModel) body;
			if (rect.getWidth() != -1 && rect.getHeight() != -1) {
				if (hasOffset) {
					sb.append(tabs + varname + ".body.setSize(" + rect.getWidth() + ", " + rect.getHeight() + ", "
							+ rect.getOffsetX() + ", " + rect.getOffsetY() + ");\n");
				} else {
					sb.append(tabs + varname + ".body.setSize(" + rect.getWidth() + ", " + rect.getHeight() + ");\n");
				}
			}
			break;
		default:
			break;
		}

		generateCommonArcadeProps(indent, sb, model);
	}

	@SuppressWarnings("boxing")
	private static void generateCommonArcadeProps(int indent, StringBuilder sb, BaseSpriteModel model) {
		String tabs = tabs(indent);
		String varname = model.getEditorName();
		ArcadeBodyModel body = model.getArcadeBody();

		class Prop {
			private String name;
			private Object def;
			private Function<ArcadeBodyModel, Object> get;

			public Prop(String name, Function<ArcadeBodyModel, Object> get, Object def) {
				super();
				this.name = name;
				this.def = def;
				this.get = get;
			}

			public void gen() {
				Object v = get.apply(body);
				if (!v.equals(def)) {
					sb.append(tabs + varname + ".body." + name + " = " + v + ";\n");
				}
			}
		}

		Prop[] props = {

				new Prop("mass", ArcadeBodyModel::getMass, 1d),

				new Prop("moves", ArcadeBodyModel::isMoves, true),

				new Prop("immovable", ArcadeBodyModel::isImmovable, false),

				new Prop("collideWorldBounds", ArcadeBodyModel::isCollideWorldBounds, false),

				new Prop("allowRotation", ArcadeBodyModel::isAllowRotation, true),

				new Prop("allowGravity", ArcadeBodyModel::isAllowGravity, true),

				new Prop("bounce.x", ArcadeBodyModel::getBounceX, 0d),

				new Prop("bounce.y", ArcadeBodyModel::getBounceY, 0d),

				new Prop("velocity.x", ArcadeBodyModel::getVelocityX, 0d),

				new Prop("velocity.y", ArcadeBodyModel::getVelocityY, 0d),

				new Prop("maxVelocity.x", ArcadeBodyModel::getMaxVelocityX, 10_000d),

				new Prop("maxVelocity.y", ArcadeBodyModel::getMaxVelocityY, 10_000d),

				new Prop("acceleration.x", ArcadeBodyModel::getAccelerationX, 0d),

				new Prop("acceleration.y", ArcadeBodyModel::getAccelerationY, 0d),

				new Prop("drag.x", ArcadeBodyModel::getDragX, 0d),

				new Prop("drag.y", ArcadeBodyModel::getDragY, 0d),

				new Prop("gravity.x", ArcadeBodyModel::getGravityX, 0d),

				new Prop("gravity.y", ArcadeBodyModel::getGravityY, 0d),

				new Prop("friction.x", ArcadeBodyModel::getFrictionX, 1d),

				new Prop("friction.y", ArcadeBodyModel::getFrictionY, 0d),

				new Prop("angularVelocity", ArcadeBodyModel::getAngularVelocity, 0d),

				new Prop("maxAngular", ArcadeBodyModel::getMaxAngular, 1000d),

				new Prop("angularAcceleration", ArcadeBodyModel::getAngularAcceleration, 0d),

				new Prop("angularDrag", ArcadeBodyModel::getAngularDrag, 0d),

				new Prop("checkCollision.none", ArcadeBodyModel::isCheckCollisionNone, false),

				new Prop("checkCollision.up", ArcadeBodyModel::isCheckCollisionUp, true),

				new Prop("checkCollision.down", ArcadeBodyModel::isCheckCollisionDown, true),

				new Prop("checkCollision.left", ArcadeBodyModel::isCheckCollisionLeft, true),

				new Prop("checkCollision.right", ArcadeBodyModel::isCheckCollisionRight, true),

				new Prop("skipQuadTree", ArcadeBodyModel::isSkipQuadTree, false),

		};

		for (Prop prop : props) {
			prop.gen();
		}

	}

	private static void generateTileProps(int indent, StringBuilder sb, TileSpriteModel model) {
		String tabs = tabs(indent);
		String varname = model.getEditorName();

		if (model.getTilePositionX() != 0 || model.getTilePositionY() != 0) {
			sb.append(tabs + varname + ".tilePosition.setTo(" + round(model.getTilePositionX()) + ", "
					+ round(model.getTilePositionY()) + ");\n");
		}

		if (model.getTileScaleX() != 1 || model.getTileScaleY() != 1) {
			sb.append(tabs + varname + ".tileScale.setTo(" + model.getTileScaleX() + ", " + model.getTileScaleY()
					+ ");\n");
		}

	}

	private void generateGroup(int indent, StringBuilder sb, GroupModel group) {
		String tabs = tabs(indent);

		{
			sb.append(tabs);
			sb.append("var " + group.getEditorName() + " = ");
			if (group.isPhysicsGroup()) {
				sb.append("this.game.add.physicsGroup(" + group.getPhysicsBodyType().getPhaserName() + ", "
						+ (group.getParent().isWorldModel() ? "this" : group.getParent().getEditorName()) + ");\n");
			} else {
				sb.append(format("this.game.add.group(%s);\n",
						group.getParent().isWorldModel() ? "this" : group.getParent().getEditorName()));

			}
		}

		generateDisplayProps(indent, sb, group);
		generateGroupProps(indent, sb, group);

		if (!group.getChildren().isEmpty()) {
			sb.append("\n");
			int i = 0;
			int last = group.getChildren().size() - 1;

			for (BaseObjectModel child : group.getChildren()) {
				generateObjectCreate(indent, sb, child);
				if (i < last) {
					sb.append("\n");
				}
				i++;
			}
		}
	}

	private static void generateGroupProps(int indent, StringBuilder sb, GroupModel model) {
		String tabs = tabs(indent);
		String varname = model.getEditorName();

		if (model.getPhysicsSortDirection() != PhysicsSortDirection.NULL) {
			sb.append(tabs + varname + ".physicsSortDirection = " + model.getPhysicsSortDirection().getPhaserName()
					+ ";\n");
		}
	}

	protected static String tabs(int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < indent; i++) {
			sb.append("\t");
		}
		return sb.toString();
	}

	private static String frameKey(IAssetFrameModel frame) {
		if (frame == null) {
			return "null";
		}

		if (frame instanceof SpritesheetAssetModel.FrameModel) {
			return Integer.toString(((SpritesheetAssetModel.FrameModel) frame).getIndex());
		}

		return "'" + frame.getKey() + "'";
	}

	private static String emptyStringToNull(String str) {
		return str == null ? null : (str.trim().length() == 0 ? null : str);
	}

	private static String round(double x) {
		return Integer.toString((int) Math.round(x));
	}

}