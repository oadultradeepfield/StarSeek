package com.oadultradeepfield.starseek.ui.results

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.oadultradeepfield.starseek.domain.model.CelestialObject
import com.oadultradeepfield.starseek.domain.model.ObjectDetail
import com.oadultradeepfield.starseek.domain.model.ObjectType
import com.oadultradeepfield.starseek.ui.theme.StarSeekTheme

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ObjectAccordionItemCollapsedPreview() {
  StarSeekTheme(dynamicColor = false) {
    ObjectAccordionItem(
        obj = CelestialObject("Betelgeuse", ObjectType.STAR, "Orion"),
        isExpanded = false,
        detailState = ObjectDetailState.Hidden,
        onClick = {},
    )
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ObjectAccordionItemExpandedPreview() {
  StarSeekTheme(dynamicColor = false) {
    ObjectAccordionItem(
        obj = CelestialObject("Betelgeuse", ObjectType.STAR, "Orion"),
        isExpanded = true,
        detailState =
            ObjectDetailState.Loaded(
                ObjectDetail(
                    name = "Betelgeuse",
                    type = ObjectType.STAR,
                    constellation = "Orion",
                    funFact =
                        "Betelgeuse is a red supergiant star that is one of the largest visible to the naked eye.",
                )
            ),
        onClick = {},
    )
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ObjectAccordionItemLoadingPreview() {
  StarSeekTheme(dynamicColor = false) {
    ObjectAccordionItem(
        obj = CelestialObject("Betelgeuse", ObjectType.STAR, "Orion"),
        isExpanded = true,
        detailState = ObjectDetailState.Loading,
        onClick = {},
    )
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ObjectAccordionItemErrorPreview() {
  StarSeekTheme(dynamicColor = false) {
    ObjectAccordionItem(
        obj = CelestialObject("Betelgeuse", ObjectType.STAR, "Orion"),
        isExpanded = true,
        detailState = ObjectDetailState.Error("Failed to load object details"),
        onClick = {},
    )
  }
}
