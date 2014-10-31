precision mediump float;

uniform vec3 u_LightPos;
uniform sampler2D u_Texture;
uniform lowp float u_TextureFlag;
uniform vec4 u_Color;
  
varying vec3 v_Position;
varying vec3 v_Normal;
varying vec2 v_TexCoordinate;

void main()                    		
{                              
	// Will be used for attenuation.
    float distance = length(u_LightPos - v_Position);                  
	
	// Get a lighting direction vector from the light to the vertex.
    vec3 lightVector = normalize(u_LightPos - v_Position);              	

	// Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
	// pointing in the same direction then it will get max illumination.
    float diffuse = max(dot(v_Normal, lightVector), 0.0);               	  		  													  

	// Add attenuation. 
    diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance)));
    
    // Add ambient lighting
    diffuse = diffuse + 0.7;

	// Multiply the color by the diffuse illumination level and texture value to get final output color.
    // The texture flag enable (or disable) the use of the texture.
    gl_FragColor = u_TextureFlag * u_Color * diffuse * texture2D(u_Texture, v_TexCoordinate) +
                   (1.0 - u_TextureFlag) * u_Color * diffuse;
  }                                                                     	

