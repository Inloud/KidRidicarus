package kidridicarus.common.metaagent.tiledmap.drawlayer;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import kidridicarus.agency.Agency.AgentHooks;
import kidridicarus.agency.agent.AgentDrawListener;
import kidridicarus.agency.tool.AllowOrder;
import kidridicarus.agency.tool.Eye;
import kidridicarus.agency.tool.ObjectProperties;
import kidridicarus.common.agent.general.CorpusAgent;
import kidridicarus.common.info.CommonInfo;
import kidridicarus.common.info.CommonKV;
import kidridicarus.common.tool.AP_Tool;
import kidridicarus.common.tool.DrawOrderAlias;

public class DrawLayerAgent extends CorpusAgent {
	private Rectangle bounds;
	private TiledMapTileLayer drawLayer;

	public DrawLayerAgent(AgentHooks agentHooks, ObjectProperties properties) {
		super(agentHooks, properties);
		this.bounds = AP_Tool.getBounds(properties);
		drawLayer = properties.get(CommonKV.AgentMapParams.KEY_TILEDMAP_TILELAYER, null, TiledMapTileLayer.class);
		if(drawLayer == null)
			throw new IllegalArgumentException("Agents needs TiledMapTileLayer in its construction properties.");
		agentHooks.addDrawListener(getDrawOrderForLayer(drawLayer, CommonInfo.KIDRID_DRAWORDER_ALIAS),
				new AgentDrawListener() {
				@Override
				public void draw(Eye eye) { eye.draw(drawLayer); }
			});
	}

	/*
	 * Returns draw order none if draw order not found for given layer,
	 * otherwise returns a draw order object based on the layer's draw order property.
	 */
	private AllowOrder getDrawOrderForLayer(TiledMapTileLayer layer, DrawOrderAlias[] drawOrderAliasList) {
		// does the layer contain a draw order key with a float value?
		Float drawOrderFloat = null;
		try {
			drawOrderFloat = layer.getProperties().get(CommonKV.Layer.KEY_LAYER_DRAWORDER, null, Float.class);
		}
		catch(ClassCastException cce1) {
			// no float value, does the layer contain a draw order key with a string value?
			String drawOrderStr = null;
			try {
				drawOrderStr = layer.getProperties().get(CommonKV.Layer.KEY_LAYER_DRAWORDER, null, String.class);
			}
			catch(ClassCastException cce2) {
				// return null because no float value and no string found to indicate draw order for layer
				return CommonInfo.DrawOrder.NONE;
			}
			// check draw order aliases to translate to draw order object
			return DrawOrderAlias.getDrawOrderForAlias(drawOrderAliasList, drawOrderStr);
		}
		if(drawOrderFloat == null)
			return CommonInfo.DrawOrder.NONE;
		return new AllowOrder(true, drawOrderFloat);
	}

	@Override
	protected Vector2 getPosition() {
		return bounds.getCenter(new Vector2());
	}

	@Override
	protected Rectangle getBounds() {
		return bounds;
	}

	public static ObjectProperties makeAP(Rectangle bounds, TiledMapTileLayer layer) {
		ObjectProperties cmProps = AP_Tool.createRectangleAP(CommonKV.AgentClassAlias.VAL_DRAWABLE_TILEMAP, bounds);
		cmProps.put(CommonKV.AgentMapParams.KEY_TILEDMAP_TILELAYER, layer);
		return cmProps;
	}
}
