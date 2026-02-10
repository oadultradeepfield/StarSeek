import {Container, getContainer} from "@cloudflare/containers";
import {env} from "cloudflare:workers";

export class StarSeekContainer extends Container<Env> {
	defaultPort = 8080;
	sleepAfter = "2m";
	envVars = {
		ASTROMETRY_API_KEY: env.NOVA_API_KEY,
		GEMINI_API_KEY: env.GEMINI_API_KEY,
		CLOUDFLARE_ACCOUNT_ID: env.CLOUDFLARE_ACCOUNT_ID,
		CLOUDFLARE_NAMESPACE_ID: env.CLOUDFLARE_NAMESPACE_ID,
		CLOUDFLARE_API_TOKEN: env.CLOUDFLARE_API_TOKEN,
	};
}

export default {
	async fetch(request: Request, env: Env): Promise<Response> {
		const container = getContainer(env.STARSEEK_CONTAINER);
		return await container.fetch(request);
	},
};
