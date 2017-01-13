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
import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.CanvasType;
import phasereditor.canvas.core.CircleArcadeBodyModel;
import phasereditor.canvas.core.GroupModel;
import phasereditor.canvas.core.ImageSpriteModel;
import phasereditor.canvas.core.PhysicsType;
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
public abstract class JSLikeCodeGenerator extends BaseCodeGenerator {

	protected final String PRE_INIT_CODE_BEGIN = "/* --- pre-init-begin --- */";
	protected final String PRE_INIT_CODE_END = "/* --- pre-init-end --- */";
	protected final String POST_INIT_CODE_BEGIN = "/* --- post-init-begin --- */";
	protected final String POST_INIT_CODE_END = "/* --- post-init-end --- */";
	protected final String END_GENERATED_CODE = "/* --- end generated code --- */";

	public JSLikeCodeGenerator(CanvasModel model) {
		super(model);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * phasereditor.canvas.core.codegen.BaseCodeGenerator#internalGenerate()
	 */
	@Override
	protected void internalGenerate() {
		line("// Generated by " + LicCore.PRODUCT_NAME);

		generateHeader();

		{
			int i = 0;
			int last = _world.getChildren().size() - 1;
			for (BaseObjectModel child : _world.getChildren()) {
				generateObjectCreate(child);
				if (i < last) {
					line();
				}
				i++;
			}
		}

		line();

		int mark1 = length();

		// public fields
		_world.walk(obj -> {
			if (!(obj instanceof WorldModel) && obj.isEditorGenerate()) {
				if (obj.isEditorPublic()) {
					String name = obj.getEditorName();
					String camel = getPublicFieldName(name);
					line("this." + camel + " = " + name + ";");
				}

				if (obj instanceof BaseSpriteModel) {
					List<AnimationModel> anims = ((BaseSpriteModel) obj).getAnimations();
					for (AnimationModel anim : anims) {
						if (anim.isPublic()) {
							String animvar = getAnimationVarName(obj, anim);
							String name = getPublicFieldName(animvar);
							line("this." + name + " = " + animvar + ";");
						}
					}
				}
			}
		});

		int mark2 = length();

		if (mark1 < mark2) {
			line("// public fields");
			line();
			append(cut(mark1, mark2));
		}

		line();

		generateFooter();
	}

	protected String getYouCanInsertCodeHere() {
		return getYouCanInsertCodeHere("user code here");
	}

	protected String getYouCanInsertCodeHere(String msg) {
		return "\n" + getIndentTabs() + "// -- " + msg + " --\n" + getIndentTabs();
	}

	protected abstract void generateFooter();

	protected abstract void generateHeader();

	protected static String getPublicFieldName(String name) {
		return "f" + name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	protected void generateObjectCreate(BaseObjectModel model) {
		if (!model.isEditorGenerate()) {
			return;
		}

		if (model instanceof GroupModel) {
			generateGroup((GroupModel) model);
		} else if (model instanceof BaseSpriteModel) {
			generateSprite((BaseSpriteModel) model);
		}
	}

	protected final String getSystemsContainerChain() {
		switch (_model.getType()) {
		case STATE:
			return "this";
		case GROUP:
		case SPRITE:
		default:
			return "this.game";
		}
	}

	private void generateSprite(BaseSpriteModel model) {

		// properties

		int mark1 = length();

		generateDisplayProps(model);

		generateSpriteProps(model);

		if (model instanceof TileSpriteModel) {
			generateTileProps((TileSpriteModel) model);
		}

		int mark2 = length();

		// create method

		String parVar = model.getParent().isWorldModel() ? "this" : model.getParent().getEditorName();

		if (mark1 < mark2 || model.isEditorPublic()) {
			append("var " + model.getEditorName() + " = ");
		}

		append(getSystemsContainerChain() + ".add.");

		boolean isState = _model.getType() == CanvasType.STATE;

		if (model instanceof ImageSpriteModel) {
			ImageSpriteModel image = (ImageSpriteModel) model;
			append("sprite(" + // sprite
					round(image.getX())// x
					+ ", " + round(image.getY()) // y
					+ ", '" + image.getAssetKey().getKey() + "'" // key
					+ (isState ? "" : ", null") // frame
					+ (isState ? "" : ", " + parVar) // group
					+ ")");
		} else if (model instanceof SpritesheetSpriteModel || model instanceof AtlasSpriteModel) {
			AssetSpriteModel<?> sprite = (AssetSpriteModel<?>) model;
			IAssetKey frame = sprite.getAssetKey();
			String frameValue = frame instanceof SpritesheetAssetModel.FrameModel
					? Integer.toString(((SpritesheetAssetModel.FrameModel) frame).getIndex())
					: "'" + frame.getKey() + "'";
			append("sprite(" + // sprite
					round(sprite.getX())// x
					+ ", " + round(sprite.getY()) // y
					+ ", '" + sprite.getAssetKey().getAsset().getKey() + "'" // key
					+ ", " + frameValue // frame
					+ (isState ? "" : ", " + parVar) // group
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

			append("button(" + // sprite
					round(button.getX())// x
					+ ", " + round(button.getY()) // y
					+ ", '" + button.getAssetKey().getAsset().getKey() + "'" // key
					+ ", " + emptyStringToNull(button.getCallback()) // callback
					+ ", " + emptyStringToNull(button.getCallbackContext()) // context
					+ ", " + frameKey(button.getOverFrame())// overFrame
					+ ", " + outFrameKey// outFrame
					+ ", " + frameKey(button.getDownFrame())// downFrame
					+ ", " + frameKey(button.getUpFrame())// upFrame
					+ (isState ? "" : ", " + parVar) // group
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

			append("tileSprite(" + // sprite
					round(tile.getX())// x
					+ ", " + round(tile.getY()) // y
					+ ", " + round(tile.getWidth()) // width
					+ ", " + round(tile.getHeight()) // height
					+ ", '" + tile.getAssetKey().getAsset().getKey() + "'" // key
					+ (isState && frame.equals("null") ? "" : ", " + frame) // frame
					+ (isState ? "" : ", " + parVar) // group
					+ ")");
		}
		line(";");

		String props = cut(mark1, mark2);
		append(props);
	}

	private void generateDisplayProps(BaseObjectModel model) {
		String varname = model.getEditorName();

		if (model instanceof GroupModel) {
			if (model.getX() != 0 || model.getY() != 0) {
				line(varname + ".position.setTo(" + round(model.getX()) + ", " + round(model.getY()) + ");");
			}
		}

		if (model.getAngle() != 0) {
			line(varname + ".angle = " + model.getAngle() + ";");
		}

		if (model.getScaleX() != 1 || model.getScaleY() != 1) {
			line(varname + ".scale.setTo(" + model.getScaleX() + ", " + model.getScaleY() + ");");
		}

		if (model.getPivotX() != 0 || model.getPivotY() != 0) {
			line(varname + ".pivot.setTo(" + model.getPivotX() + ", " + model.getPivotY() + ");");
		}
	}

	private void generateSpriteProps(BaseSpriteModel model) {
		String varname = model.getEditorName();

		if (model.getAnchorX() != 0 || model.getAnchorY() != 0) {
			line(varname + ".anchor.setTo(" + model.getAnchorX() + ", " + model.getAnchorY() + ");");
		}

		if (model.getTint() != null && !model.getTint().equals("0xffffff")) {
			line(varname + ".tint = " + model.getTint() + ";");
		}

		if (!model.getAnimations().isEmpty()) {
			for (AnimationModel anim : model.getAnimations()) {
				String animvar = null;
				if (anim.isPublic() || anim.isKillOnComplete()) {
					animvar = getAnimationVarName(model, anim);
					append("var " + animvar + " = ");
				}

				append(varname + ".animations.add(");

				append("'" + anim.getName() + "', [");
				int i = 0;
				for (IAssetFrameModel frame : anim.getFrames()) {
					if (i++ > 0) {
						append(", ");
					}
					if (frame instanceof SpritesheetAssetModel.FrameModel) {
						append(frame.getKey());
					} else {
						append("'" + frame.getKey() + "'");
					}
				}
				line("], " + anim.getFrameRate() + ", " + anim.isLoop() + ");");

				if (anim.isKillOnComplete()) {
					line(animvar + ".killOnComplete = true;");
				}
			}
		}

		generateBodyProps(model);

		// always generate data at the end, because it can use previous
		// properties.

		String data = model.getData();
		if (data != null && data.trim().length() > 0) {
			data = data.replace("$$", varname);
			data = data.replace("\n", "\n" + getIndentTabs());
			line(varname + ".data = " + data + ";");
		}
	}

	private static String getAnimationVarName(BaseObjectModel obj, AnimationModel anim) {
		return obj.getEditorName() + "_" + anim.getName();
	}

	private void generateBodyProps(BaseSpriteModel model) {
		BodyModel body = model.getBody();
		if (body != null) {
			if (body instanceof ArcadeBodyModel) {
				generateArcadeBodyProps(model);
			}
		}
	}

	private void generateArcadeBodyProps(BaseSpriteModel model) {
		String varname = model.getEditorName();

		if (!model.getParent().isPhysicsGroup() || model.getParent().getPhysicsBodyType() != PhysicsType.ARCADE) {
			line("this.game.physics.arcade.enable(" + varname + ");");
		}

		ArcadeBodyModel body = model.getArcadeBody();
		boolean hasOffset = body.getOffsetX() != 0 || body.getOffsetY() != 0;
		switch (body.getBodyType()) {
		case ARCADE_CIRCLE:
			CircleArcadeBodyModel circle = (CircleArcadeBodyModel) body;
			if (hasOffset) {
				line(varname + ".body.setCircle(" + circle.getRadius() + ", " + circle.getOffsetX() + ", "
						+ circle.getOffsetY() + ");");
			} else {
				line(varname + ".body.setCircle(" + circle.getRadius() + ");");
			}
			break;
		case ARCADE_RECT:
			RectArcadeBodyModel rect = (RectArcadeBodyModel) body;
			if (rect.getWidth() != -1 && rect.getHeight() != -1) {
				if (hasOffset) {
					line(varname + ".body.setSize(" + rect.getWidth() + ", " + rect.getHeight() + ", "
							+ rect.getOffsetX() + ", " + rect.getOffsetY() + ");");
				} else {
					line(varname + ".body.setSize(" + rect.getWidth() + ", " + rect.getHeight() + ");");
				}
			}
			break;
		default:
			break;
		}

		generateCommonArcadeProps(model);
	}

	@SuppressWarnings("boxing")
	private void generateCommonArcadeProps(BaseSpriteModel model) {
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
					line(varname + ".body." + name + " = " + v + ";");
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

	private void generateTileProps(TileSpriteModel model) {
		String varname = model.getEditorName();

		if (model.getTilePositionX() != 0 || model.getTilePositionY() != 0) {
			line(varname + ".tilePosition.setTo(" + round(model.getTilePositionX()) + ", "
					+ round(model.getTilePositionY()) + ");");
		}

		if (model.getTileScaleX() != 1 || model.getTileScaleY() != 1) {
			line(varname + ".tileScale.setTo(" + model.getTileScaleX() + ", " + model.getTileScaleY() + ");");
		}

	}

	private void generateGroup(GroupModel group) {

		{
			append("var " + group.getEditorName() + " = ");
			if (group.isPhysicsGroup()) {
				line(getSystemsContainerChain() + ".add.physicsGroup(" + group.getPhysicsBodyType().getPhaserName()
						+ ", " + (group.getParent().isWorldModel() ? "this" : group.getParent().getEditorName())
						+ ");");
			} else {
				line(String.format(getSystemsContainerChain() + ".add.group(%s);",
						group.getParent().isWorldModel() ? "this" : group.getParent().getEditorName()));

			}
		}

		generateDisplayProps(group);
		generateGroupProps(group);

		if (!group.getChildren().isEmpty()) {
			line();
			int i = 0;
			int last = group.getChildren().size() - 1;

			for (BaseObjectModel child : group.getChildren()) {
				generateObjectCreate(child);
				if (i < last) {
					line();
				}
				i++;
			}
		}
	}

	private void generateGroupProps(GroupModel model) {
		String varname = model.getEditorName();

		if (model.getPhysicsSortDirection() != PhysicsSortDirection.NULL) {
			line(varname + ".physicsSortDirection = " + model.getPhysicsSortDirection().getPhaserName() + ";");
		}
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

}
