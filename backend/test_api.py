import argparse
import glob
import os
import requests
import time
import re
from PIL import Image, ImageDraw, ImageFont

def test_api(api_url, images_dir):
    image_paths = glob.glob(os.path.join(images_dir, "*.[jp][pn]g"))
    if not image_paths:
        print(f"No images found in {images_dir}")
        return

    output_dir = "output_comparisons"
    os.makedirs(output_dir, exist_ok=True)

    try:
        font = ImageFont.truetype("Roboto-Regular.ttf", 26)
        title_font = ImageFont.truetype("Roboto-Regular.ttf", 32)
        box_font = ImageFont.truetype("Roboto-Regular.ttf", 40)
    except:
        font = ImageFont.load_default()
        title_font = font
        box_font = font

    parameters = {
        "temperature": 0.0,
        "top_p": 1.0,
        "repetition_penalty": 1.0,
        "layout_threshold": 0.2,
        "layout_nms": "true",
        "max_new_tokens": 2048,
        "use_doc_orientation_classify": "false",
        "use_doc_unwarping": "false",
        "use_layout_detection": "true",
        "use_chart_recognition": "false",
        "use_seal_recognition": "false",
        "use_ocr_for_image_block": "false",
        "format_block_content": "true",
        "merge_layout_blocks": "true"
    }

    print(f"Testing API at: {api_url}")
    print(f"Found {len(image_paths)} images to process.")
    print(f"Comparisons will be saved to: {output_dir}/")
    print("-" * 50)

    for img_path in image_paths:
        img_name = os.path.basename(img_path)
        print(f"\nProcessing {img_name}...")
        
        start_time = time.time()
        try:
            with open(img_path, "rb") as f:
                files = {"file": (img_name, f, "image/jpeg")}
                response = requests.post(
                    f"{api_url}/api/ocr",
                    files=files,
                    data=parameters,
                    timeout=60
                )
            
            end_time = time.time()
            elapsed = end_time - start_time
            
            if response.status_code == 200:
                result = response.json()
                print(f"[SUCCESS] Success ({elapsed:.2f}s)")
                
                # Filter boxes to only include text-related ones
                all_boxes = result.get("boxes", [])
                ignore_labels = ['image', 'header_image', 'footer_image', 'figure']
                text_boxes = [b for b in all_boxes if b.get('label') not in ignore_labels]
                
                # Order boxes based on 'order' field
                ordered_boxes = [b for b in text_boxes if b.get('order') is not None]
                ordered_boxes.sort(key=lambda x: x.get('order'))
                
                # The backend now returns 'content' in each box (Option C)
                # We can just iterate over all text boxes.
                
                # --- Create 3-Panel Side-by-Side Comparison ---
                try:
                    original_img = Image.open(img_path).convert("RGB")
                    boxed_img = original_img.copy()
                    draw_box = ImageDraw.Draw(boxed_img)
                    
                    w, h = original_img.size
                    
                    # Panel 3: Text and Probabilities
                    text_panel = Image.new('RGB', (max(w, 800), max(h, 1000)), color=(250, 250, 250))
                    draw_text = ImageDraw.Draw(text_panel)
                    panel_w = text_panel.width
                    
                    y_offset = 20
                    draw_text.text((20, y_offset), "--- EXTRACTED CONTENT ---", fill="black", font=title_font)
                    y_offset += 60
                    
                    char_width = 15 # Approx
                    max_chars = max(15, (panel_w - 40) // char_width)
                    
                    # Interleave boxes and text blocks
                    # Not all boxes might have ordered matches, so we process ordered_boxes 
                    for i, box in enumerate(ordered_boxes):
                        score = box.get('score', 0)
                        label = box.get('label', 'text')
                        
                        # Draw box on image
                        if 'coordinate' in box and len(box['coordinate']) == 4:
                            x1, y1, x2, y2 = box['coordinate']
                            draw_box.rectangle([x1, y1, x2, y2], outline="red", width=5)
                            draw_box.rectangle([x1, max(0, y1-40), x1+60, y1], fill="red")
                            draw_box.text((x1+5, max(0, y1-40)), f"#{i+1}", fill="white", font=box_font)
                        
                        # Write on panel
                        header = f"Box #{i+1} ({label}) - Score: {score:.1%}"
                        draw_text.text((20, y_offset), header, fill="black", font=title_font)
                        y_offset += 40
                        
                        # Write text content
                        content = box.get('content', '<No parsed content found>')
                        
                        # Word wrap content
                        words = content.split(' ')
                        lines = []
                        current_line = []
                        current_len = 0
                        for word in words:
                            if current_len + len(word) > max_chars:
                                lines.append(" ".join(current_line))
                                current_line = [word]
                                current_len = len(word)
                            else:
                                current_line.append(word)
                                current_len += len(word) + 1
                        if current_line:
                            lines.append(" ".join(current_line))
                            
                        for line in lines:
                            draw_text.text((40, y_offset), line, fill="#1e3a8a", font=font)
                            y_offset += 35
                            
                        y_offset += 20 # Space between boxes

                    # Combine into 3-panel image
                    # Use actual text panel dimensions which might be taller than original image
                    final_h = max(h, y_offset + 50)
                    comparison = Image.new('RGB', (w * 2 + panel_w, final_h), color=(250, 250, 250))
                    comparison.paste(original_img, (0, 0))
                    comparison.paste(boxed_img, (w, 0))
                    comparison.paste(text_panel, (w * 2, 0))
                    
                    out_path = os.path.join(output_dir, f"compare_{img_name}")
                    comparison.save(out_path)
                    print(f"  Saved 3-panel comparison image: {out_path}")
                except Exception as img_e:
                    print(f"  [ERROR] Failed to generate comparison image: {img_e}")
                
            else:
                print(f"[ERROR] Error HTTP {response.status_code} ({elapsed:.2f}s)")
                print(response.text)
                
        except requests.exceptions.RequestException as e:
            print(f"[ERROR] Connection Error: {e}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", type=str, required=True)
    parser.add_argument("--images_dir", type=str, required=True)
    args = parser.parse_args()
    
    base_url = args.url.rstrip('/')
    test_api(base_url, args.images_dir)
