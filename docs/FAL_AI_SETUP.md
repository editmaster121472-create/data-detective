# fal.ai setup for Data Detective

The Creative Studio uses fal.ai for text-to-image and text-to-video generation.

## 1. Create a server API key

1. Sign in to fal.ai.
2. Open the API Keys page: https://fal.ai/dashboard/keys
3. Create a key and copy it once.

The ChatGPT fal connector and the PythonAnywhere website use separate authentication. Connecting fal to ChatGPT does not automatically give the website access to the fal account.

## 2. Add the key privately on PythonAnywhere

In the website's private `.env` file, add:

```env
FAL_KEY=your_fal_api_key_here
FAL_IMAGE_MODEL=google/nano-banana-2.1
FAL_VIDEO_MODEL=minimax/h3-max/text-to-video
```

Do not commit the real key to GitHub, put it in HTML/JavaScript, or send it in chat.

## 3. Reload and test

Reload the PythonAnywhere web app. Open the AI Studio section:
- Image Studio: enter a prompt and choose an aspect ratio.
- Video Studio: enter a prompt and choose duration and aspect ratio.

Image generation returns an image URL. Video generation is queued and the browser checks the same job until it completes.

## Billing

fal.ai is pay-as-you-go. Image and video requests can incur charges. Check the model's current pricing and your account balance before generating. The website does not automatically purchase credits.
