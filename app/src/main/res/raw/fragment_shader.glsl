precision mediump float;

// Contains positions for all lights
uniform vec3 u_LightPos;
/*uniform vec3 u_LightPos1;
uniform vec3 u_LightPos2;
uniform vec3 u_LightPos3;
uniform vec3 u_LightPos4;*/

uniform sampler2D u_Texture;
uniform lowp vec4 u_Color;

uniform lowp float u_TextureFlag;
uniform lowp float u_FogFlag;
  
varying highp vec3 v_Position;
varying vec3 v_Normal;
varying vec2 v_TexCoordinate;

void main()                    		
{
    // Compute the lights depending on all light sources.
    /*// The dot product is used to get the current illumination (higher when pointing to the same direction)
    float diffuse = max(dot(v_Normal, normalize(u_LightPos1 - v_Position)), 0.0)
                    + max(dot(v_Normal, normalize(u_LightPos2 - v_Position)), 0.0)
                    + max(dot(v_Normal, normalize(u_LightPos3 - v_Position)), 0.0)
                    + max(dot(v_Normal, normalize(u_LightPos4 - v_Position)), 0.0);

	// Add attenuation. 
    diffuse = diffuse * (1.0 / (1.0 + (0.25 * length(u_LightPos1 - v_Position))));
                                            //* length(u_LightPos2 - v_Position)
                                            //* length(u_LightPos3 - v_Position)
                                            //* length(u_LightPos4 - v_Position))));*/

    float distance = length(u_LightPos - v_Position);
    vec3 lightVec = normalize(u_LightPos - v_Position);
    float diffuse = max(dot(v_Normal, lightVec), 0.0);
    diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance)));
    
    // Add ambient lighting
    diffuse = diffuse + 0.4;

	// Multiply the color by the diffuse illumination level and texture value to get final output color.
    // The texture flag enable (or disable) the texture.
    vec4 finalColor = u_TextureFlag * u_Color * diffuse * texture2D(u_Texture, v_TexCoordinate) +
                            (1.0 - u_TextureFlag) * u_Color * diffuse;

    // Compute the fog
    const float LOG2 = 1.442695;
    const float fogDensity = 0.01;
    const vec4 fogColor = vec4(0.03, 0.03, 0.03, 0.0);

    float z = length(v_Position);
    float fogFactor = exp2(-fogDensity * fogDensity * z * z * LOG2);
    fogFactor = clamp(fogFactor, 0.0, 1.0);

    gl_FragColor = u_FogFlag * mix(fogColor, finalColor, fogFactor) + (1.0 - u_FogFlag) * finalColor;
}