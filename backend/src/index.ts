import {Container, getContainer} from "@cloudflare/containers";
import {env} from "cloudflare:workers";

export class StarSeekContainer extends Container<Env> {
	defaultPort = 8080;
	sleepAfter = "2m";
	envVars = {
		ASTROMETRY_API_KEY: env.NOVA_API_KEY,
	};
}

export default {
	async fetch(request: Request, env: Env): Promise<Response> {
		const container = getContainer(env.STARSEEK_CONTAINER);
		return await container.fetch(request);
	},
};
