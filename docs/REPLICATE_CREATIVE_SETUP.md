# Replicate Image and Video Studio setup

The app reads the Replicate credential only on the Flask server. Never put the token in HTML, JavaScript, GitHub, screenshots, or chat.

Add these lines to the private `.env` file in the deployed application directory:

```dotenv
REPLICATE_API_TOKEN=your_private_token_here
# Optional model overrides:
# REPLICATE_IMAGE_MODEL=black-forest-labs/flux-schnell
# REPLICATE_VIDEO_MODEL=minimax/video-01
```

The default image model is `black-forest-labs/flux-schnell`; the default video model is `minimax/video-01`. Model availability, input schemas, and pricing can change. Override the model slug in `.env` if needed.

After changing `.env`, reload the PythonAnywhere web app. Open Image Studio or Video Studio and check the connection status. The API creates asynchronous Replicate predictions and polls their status from the server. Prediction IDs are kept in process memory, so an app restart clears active browser polling records.

The optional reference image accepts PNG, JPEG, or WEBP up to 7 MB. The browser receives generated output URLs, not the API token. Replicate output URLs are provider-hosted and may expire; save generated files promptly.

**Cost note:** trial availability and credits depend on Replicate's current account and model terms. Generation is not guaranteed to remain free.


## Optional free daily image provider (Cloudflare Workers AI)

Add these private environment values to the deployed server `.env` file:

```dotenv
CLOUDFLARE_ACCOUNT_ID=your_account_id
CLOUDFLARE_API_TOKEN=your_workers_ai_token
```

When both values are present, Image Studio automatically uses Cloudflare's hosted `@cf/black-forest-labs/flux-1-schnell` model. The Workers Free plan includes 10,000 Neurons per day; requests fail after the free daily allowance is exhausted. Video generation remains on Replicate and may cost money. Cloudflare's FLUX.1 schnell route is text-to-image; reference-image editing remains available only through Replicate. Never commit credentials to GitHub or expose them in browser code.
