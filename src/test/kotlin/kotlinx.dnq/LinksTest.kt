package kotlinx.dnq

import com.google.common.truth.Truth.assertThat
import kotlinx.dnq.query.*
import kotlinx.dnq.util.getAddedLinks
import kotlinx.dnq.util.getOldValue
import kotlinx.dnq.util.getRemovedLinks
import org.hamcrest.CustomMatcher
import org.hamcrest.collection.IsEmptyCollection
import org.hamcrest.collection.IsIterableContainingInOrder
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class LinksTest : DBTest() {

    @Test
    fun `bidirectional many to many`() {
        store.transactional {
            val admin = User.new { login = "anakin"; skill = 1 }
            RootGroup.new {
                name = "Root"
                nestedGroups.add(NestedGroup.new { name = "A"; owner = admin })
                nestedGroups.add(NestedGroup.new { name = "B"; owner = admin })
                nestedGroups.add(NestedGroup.new { name = "C"; owner = admin })
            }
        }

        store.transactional {
            val root = RootGroup.query(RootGroup::name eq "Root").first()
            assertEquals(3, root.nestedGroups.size())
            listOf("a", "b", "c").forEach {
                assertTrue(root.nestedGroups.any(NestedGroup::name eq it))
                assertFalse(root.entity.getLinks("nested").isEmpty)
            }
            root.nestedGroups.asSequence().forEach {
                assertEquals(root, it.parentGroup)
                assertEquals(root.entity, it.entity.getLink("parent"))
            }
        }
    }

    @Test
    fun `bidirectional many to one`() {
        val contact = store.transactional {
            Contact.new {
                this.email = "1@1.com"
                this.user = User.new {
                    login = "1"
                    skill = 1
                }
            }
        }

        store.transactional {
            assertEquals("1", contact.user.login)
        }
    }

    @Test
    fun `bidirectional one to many`() {
        val user = store.transactional {
            User.new {
                login = "1"
                skill = 1
                contacts.add(Contact.new {
                    this.email = "1@1.com"
                })
            }
        }

        store.transactional {
            assertEquals("1@1.com", user.contacts.first().email)
        }
    }

    @Test
    fun `getAddedLinks should return added links`() {
        val user = store.transactional {
            User.new {
                login = "zeckson"
                skill = 1
            }
        }
        store.transactional {
            user.contacts.add(Contact.new { email = "zeckson@spb.ru" })
            assertThat(user.getAddedLinks(User::contacts).toList().map { it.email }).containsExactly("zeckson@spb.ru")
            assertThat(user.getRemovedLinks(User::contacts).toList()).isEmpty()
        }
    }

    @Test
    fun `getRemovedLinks should return removed links`() {
        val user = store.transactional {
            User.new {
                login = "zeckson"
                skill = 1
                contacts.add(Contact.new { email = "zeckson@spb.ru" })
            }
        }
        store.transactional {
            val contact = user.contacts.first()
            user.contacts.remove(contact)
            contact.delete()

            Assert.assertThat(user.getAddedLinks(User::contacts).toList(), IsEmptyCollection())
            Assert.assertThat(user.getRemovedLinks(User::contacts).toList(), IsIterableContainingInOrder(listOf(contactMatcher { it.getOldValue(Contact::email) == "zeckson@spb.ru" })))
        }
    }

    private fun contactMatcher(match: (Contact) -> Boolean) = object : CustomMatcher<DBTest.Contact>("email contact") {
        override fun matches(item: Any?) = item is Contact && match(item)
    }
}