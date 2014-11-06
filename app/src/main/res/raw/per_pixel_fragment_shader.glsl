precision mediump float;

uniform vec3 u_LightPos;
uniform sampler2D u_Texture;
uniform vec4 u_Color;

uniform lowp float u_TextureFlag;
uniform lowp float u_FogFlag;
  
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
    diffuse = diffuse + 0.5;

	// Multiply the color by the diffuse illumination level and texture value to get final output color.
    // The texture flag enable (or disable) the use of the texture.
    vec4 finalColor = u_TextureFlag * u_Color * diffuse * texture2D(u_Texture, v_TexCoordinate) +
                            (1.0 - u_TextureFlag) * u_Color * diffuse;

    // Compute the fog
    const float LOG2 = 1.442695;
    const float fogDensity = 0.035;
    const vec4 fogColor = vec4(0.02, 0.02, 0.02, 0.0);

    float z = length(v_Position);
    float fogFactor = exp2( -fogDensity * fogDensity * z * z * LOG2 );
    fogFactor = clamp(fogFactor, 0.0, 1.0);

    gl_FragColor = u_FogFlag * mix(fogColor, finalColor, fogFactor) + (1.0 - u_FogFlag) * finalColor;
}

