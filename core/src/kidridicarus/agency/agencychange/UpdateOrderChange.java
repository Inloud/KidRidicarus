package kidridicarus.agency.agencychange;

import kidridicarus.agency.tool.AllowOrder;

public class UpdateOrderChange {
	public AgentPlaceholder ap;
	public AllowOrder updateOrder;

	public UpdateOrderChange(AgentPlaceholder agent, AllowOrder updateOrder) {
		this.ap = agent;
		this.updateOrder = updateOrder;
	}
}